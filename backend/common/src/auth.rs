//! 认证与授权模块
//!
//! 提供 JWT 编解码、密码哈希/验证、以及 Axum 的 AuthUser 提取器。
//! 受保护的接口通过 AuthUser 自动从 Authorization 头提取用户信息。

use anyhow::Context;
use axum::extract::FromRequestParts;
use axum::http::request::Parts;
use jsonwebtoken::{DecodingKey, EncodingKey, Header, Validation, decode, encode};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use argon2::{
    Algorithm, Argon2, Params, Version,
    password_hash::{PasswordHash, PasswordHasher, PasswordVerifier, SaltString, rand_core::OsRng},
};

use crate::error::AppError;
use crate::state::AppState;

/// 构建显式参数的 Argon2id 实例
/// 参数取自 OWASP 2024 推荐：m=19456 KiB (19 MB), t=2, p=1
fn argon2() -> Argon2<'static> {
    let params = Params::new(19_456, 2, 1, None).expect("invalid argon2 params");
    Argon2::new(Algorithm::Argon2id, Version::V0x13, params)
}

/// JWT 载荷
#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    /// 用户 ID
    pub sub: Uuid,
    /// 角色编码
    pub role_code: String,
    /// 过期时间戳
    pub exp: usize,
}

/// 已认证用户信息，作为 Axum 提取器使用
#[derive(Debug, Clone)]
pub struct AuthUser {
    pub user_id: Uuid,
    pub role_code: String,
}

impl AuthUser {
    /// 当前用户是否为管理员
    pub fn is_admin(&self) -> bool {
        self.role_code == "admin"
    }

    /// 检查当前用户是否为管理员，否则返回 Forbidden
    pub fn require_admin(&self) -> Result<(), AppError> {
        if !self.is_admin() {
            return Err(AppError::Forbidden("insufficient permissions".into()));
        }
        Ok(())
    }
}

/// 生成 JWT Token，有效期 7 天
pub fn encode_jwt(secret: &str, user_id: Uuid, role_code: &str) -> Result<String, AppError> {
    let exp = chrono::Utc::now()
        .checked_add_signed(chrono::Duration::days(7))
        .unwrap()
        .timestamp() as usize;
    let claims = Claims {
        sub: user_id,
        role_code: role_code.into(),
        exp,
    };
    Ok(encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(secret.as_bytes()),
    )
    .context("JWT 编码失败")?)
}

/// 解码并验证 JWT Token
pub fn decode_jwt(secret: &str, token: &str) -> Result<Claims, AppError> {
    decode::<Claims>(
        token,
        &DecodingKey::from_secret(secret.as_bytes()),
        &Validation::default(),
    )
    .map(|data| data.claims)
    .map_err(|_| AppError::Unauthorized("invalid token".into()))
}

/// 从请求头中提取 Bearer Token 并解析为 AuthUser。
/// 优先从 request extensions 中获取已由 middleware 注入的 AuthUser，
/// 命中即直接返回，避免重复的 JWT 解码 + Redis 校验。
impl FromRequestParts<AppState> for AuthUser {
    type Rejection = AppError;

    async fn from_request_parts(
        parts: &mut Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        // 快路径：middleware 已验证并注入
        if let Some(cached) = parts.extensions.get::<AuthUser>() {
            return Ok(cached.clone());
        }

        // 慢路径：未经 middleware 覆盖的路由（兜底，不应常态触发）
        let header = parts
            .headers
            .get("Authorization")
            .and_then(|v| v.to_str().ok())
            .ok_or_else(|| AppError::Unauthorized("missing authorization header".into()))?;

        let token = header
            .strip_prefix("Bearer ")
            .ok_or_else(|| AppError::Unauthorized("invalid authorization format".into()))?;

        let claims = decode_jwt(&state.config.jwt_secret, token)?;

        let mut redis = state.redis.clone();
        if !crate::redis::verify_token(&mut redis, token).await? {
            return Err(AppError::Unauthorized("token has been revoked".into()));
        }

        Ok(AuthUser {
            user_id: claims.sub,
            role_code: claims.role_code,
        })
    }
}

/// 使用 Argon2id 对密码进行哈希（显式参数，OWASP 2024 推荐值）
pub fn hash_password(password: &str) -> Result<String, AppError> {
    let salt = SaltString::generate(&mut OsRng);
    Ok(argon2()
        .hash_password(password.as_bytes(), &salt)
        .map(|h| h.to_string())
        .map_err(|e| anyhow::anyhow!("密码哈希失败: {e}"))?)
}

/// 验证密码是否与哈希匹配
pub fn verify_password(password: &str, hash: &str) -> Result<bool, AppError> {
    let parsed = PasswordHash::new(hash).map_err(|e| anyhow::anyhow!("哈希解析失败: {e}"))?;
    Ok(argon2()
        .verify_password(password.as_bytes(), &parsed)
        .is_ok())
}

/// 已认证且为管理员的用户,作为 Axum 提取器使用。
///
/// 用法:handler 签名写 `admin: AdminUser` 即自动要求管理员身份,
/// 省去 `auth.require_admin()?;` 的 12+ 处重复调用。
#[derive(Debug, Clone)]
pub struct AdminUser(pub AuthUser);

impl FromRequestParts<AppState> for AdminUser {
    type Rejection = AppError;

    async fn from_request_parts(
        parts: &mut Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        let auth = AuthUser::from_request_parts(parts, state).await?;
        if !auth.is_admin() {
            return Err(AppError::Forbidden("insufficient permissions".into()));
        }
        Ok(AdminUser(auth))
    }
}

impl std::ops::Deref for AdminUser {
    type Target = AuthUser;
    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

/// 全局鉴权中间件：要求请求携带合法的 Bearer Token，未验证的 token 或已撤销的 token 一律拒绝。
/// 用于包住所有"必须登录"的路由分组，作为 `AuthUser` 提取器的兜底防线，
/// 确保即使 handler 忘记声明 `auth: AuthUser` 也不会放行匿名请求。
pub async fn require_auth_middleware(
    axum::extract::State(state): axum::extract::State<AppState>,
    mut req: axum::extract::Request,
    next: axum::middleware::Next,
) -> Result<axum::response::Response, AppError> {
    let header = req
        .headers()
        .get("Authorization")
        .and_then(|v| v.to_str().ok())
        .ok_or_else(|| AppError::Unauthorized("missing authorization header".into()))?;

    let token = header
        .strip_prefix("Bearer ")
        .ok_or_else(|| AppError::Unauthorized("invalid authorization format".into()))?;

    let claims = decode_jwt(&state.config.jwt_secret, token)?;

    let mut redis = state.redis.clone();
    if !crate::redis::verify_token(&mut redis, token).await? {
        return Err(AppError::Unauthorized("token has been revoked".into()));
    }

    // 将已验证的用户信息注入 request extensions，供 handler 以 O(1) 获取
    req.extensions_mut().insert(AuthUser {
        user_id: claims.sub,
        role_code: claims.role_code,
    });

    Ok(next.run(req).await)
}
