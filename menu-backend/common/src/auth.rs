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
    Argon2,
    password_hash::{PasswordHash, PasswordHasher, PasswordVerifier, SaltString, rand_core::OsRng},
};

use crate::error::AppError;
use crate::state::AppState;

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
    /// 检查当前用户是否为管理员，否则返回 Forbidden
    pub fn require_admin(&self) -> Result<(), AppError> {
        if self.role_code != "admin" {
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

/// 从请求头中提取 Bearer Token 并解析为 AuthUser
impl FromRequestParts<AppState> for AuthUser {
    type Rejection = AppError;

    async fn from_request_parts(
        parts: &mut Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        let header = parts
            .headers
            .get("Authorization")
            .and_then(|v| v.to_str().ok())
            .ok_or_else(|| AppError::Unauthorized("missing authorization header".into()))?;

        let token = header
            .strip_prefix("Bearer ")
            .ok_or_else(|| AppError::Unauthorized("invalid authorization format".into()))?;

        let claims = decode_jwt(&state.config.jwt_secret, token)?;

        // 检查 token 是否在 Redis 中存在（未被撤销）
        let mut redis = state.redis.clone();
        if !crate::redis::verify_token(&mut redis, token).await? {
            return Err(AppError::Unauthorized("token has been revoked".into()));
        }

        tracing::debug!(
            "用户认证通过: user_id={}, role={}",
            claims.sub,
            claims.role_code
        );
        Ok(AuthUser {
            user_id: claims.sub,
            role_code: claims.role_code,
        })
    }
}

/// 使用 Argon2 对密码进行哈希
pub fn hash_password(password: &str) -> Result<String, AppError> {
    let salt = SaltString::generate(&mut OsRng);
    let argon2 = Argon2::default();
    Ok(argon2
        .hash_password(password.as_bytes(), &salt)
        .map(|h| h.to_string())
        .map_err(|e| anyhow::anyhow!("密码哈希失败: {e}"))?)
}

/// 验证密码是否与哈希匹配
pub fn verify_password(password: &str, hash: &str) -> Result<bool, AppError> {
    let parsed = PasswordHash::new(hash).map_err(|e| anyhow::anyhow!("哈希解析失败: {e}"))?;
    Ok(Argon2::default()
        .verify_password(password.as_bytes(), &parsed)
        .is_ok())
}
