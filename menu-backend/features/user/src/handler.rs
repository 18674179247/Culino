//! 用户模块 handler
//!
//! 处理用户注册、登录、登出、获取个人信息、更新个人资料等接口。

use axum::{Json, extract::State};
use axum::http::HeaderMap;

use menu_common::auth::{AuthUser, encode_jwt, hash_password, verify_password};
use menu_common::error::AppError;
use menu_common::redis::{revoke_token, save_token};
use menu_common::response::{ApiResponse, ApiResult};
use menu_common::state::AppState;
use validator::Validate;

use crate::model::*;
use crate::repo::{PgUserRepo, UserRepo};

/// 用户注册：创建账号并返回 JWT Token
#[utoipa::path(post, path = "/api/v1/user/register", tag = "用户",
    request_body = RegisterReq,
    responses((status = 200, body = TokenResponse))
)]
pub async fn register(
    State(state): State<AppState>,
    Json(req): Json<RegisterReq>,
) -> ApiResult<TokenResponse> {
    tracing::debug!("用户注册请求: username={}", req.username);
    req.validate()?;

    let repo = PgUserRepo::new(state.pool.clone());
    let password_hash = hash_password(&req.password)?;

    let user = repo
        .create(&CreateUser {
            username: req.username,
            nickname: req.nickname,
            password_hash,
        })
        .await?;

    let role = repo.find_role_by_id(user.role_id.unwrap_or(2)).await?;
    let role_code = role.map(|r| r.code).unwrap_or_else(|| "user".into());
    let token = encode_jwt(&state.config.jwt_secret, user.id, &role_code)?;

    // 存储 token 到 Redis
    let mut redis = state.redis.clone();
    save_token(&mut redis, &token, &user.id.to_string()).await?;

    tracing::info!("用户注册成功: user_id={}", user.id);
    ApiResponse::ok(TokenResponse {
        token,
        user: UserResponse {
            id: user.id,
            username: user.username,
            nickname: user.nickname,
            avatar: user.avatar,
            role_code: Some(role_code),
            created_at: user.created_at,
        },
    })
}

/// 用户登录：验证凭据并返回 JWT Token
#[utoipa::path(post, path = "/api/v1/user/login", tag = "用户",
    request_body = LoginReq,
    responses((status = 200, body = TokenResponse))
)]
pub async fn login(
    State(state): State<AppState>,
    Json(req): Json<LoginReq>,
) -> ApiResult<TokenResponse> {
    tracing::debug!("用户登录请求: username={}", req.username);
    req.validate()?;

    let repo = PgUserRepo::new(state.pool.clone());

    let user = repo
        .find_by_username(&req.username)
        .await?
        .ok_or_else(|| {
            tracing::warn!("登录失败，用户不存在: username={}", req.username);
            AppError::Unauthorized("invalid username or password".into())
        })?;

    if !verify_password(&req.password, &user.password_hash)? {
        tracing::warn!("登录失败，密码错误: username={}", req.username);
        return Err(AppError::Unauthorized("invalid username or password".into()));
    }

    if !user.is_active.unwrap_or(true) {
        tracing::warn!("登录失败，账号已禁用: user_id={}", user.id);
        return Err(AppError::Forbidden("account is disabled".into()));
    }

    let role = repo.find_role_by_id(user.role_id.unwrap_or(2)).await?;
    let role_code = role.map(|r| r.code).unwrap_or_else(|| "user".into());
    let token = encode_jwt(&state.config.jwt_secret, user.id, &role_code)?;

    // 存储 token 到 Redis
    let mut redis = state.redis.clone();
    save_token(&mut redis, &token, &user.id.to_string()).await?;

    tracing::info!("用户登录成功: user_id={}", user.id);
    ApiResponse::ok(TokenResponse {
        token,
        user: UserResponse {
            id: user.id,
            username: user.username,
            nickname: user.nickname,
            avatar: user.avatar,
            role_code: Some(role_code),
            created_at: user.created_at,
        },
    })
}

/// 获取当前登录用户信息
#[utoipa::path(get, path = "/api/v1/user/me", tag = "用户",
    security(("bearer" = [])),
    responses((status = 200, body = UserResponse))
)]
pub async fn me(
    State(state): State<AppState>,
    auth: AuthUser,
) -> ApiResult<UserResponse> {
    tracing::debug!("获取用户信息: user_id={}", auth.user_id);

    let repo = PgUserRepo::new(state.pool.clone());
    let user = repo
        .find_by_id(auth.user_id)
        .await?
        .ok_or_else(|| AppError::NotFound("user not found".into()))?;

    ApiResponse::ok(UserResponse {
        id: user.id,
        username: user.username,
        nickname: user.nickname,
        avatar: user.avatar,
        role_code: Some(auth.role_code),
        created_at: user.created_at,
    })
}

/// 更新当前用户的个人资料（昵称、头像）
#[utoipa::path(put, path = "/api/v1/user/me", tag = "用户",
    security(("bearer" = [])),
    request_body = UpdateProfileReq,
    responses((status = 200, body = UserResponse))
)]
pub async fn update_profile(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(req): Json<UpdateProfileReq>,
) -> ApiResult<UserResponse> {
    tracing::info!("更新用户资料: user_id={}", auth.user_id);
    req.validate()?;

    let repo = PgUserRepo::new(state.pool.clone());
    let user = repo
        .update(
            auth.user_id,
            &UpdateUser {
                nickname: req.nickname,
                avatar: req.avatar,
            },
        )
        .await?;

    tracing::info!("用户资料更新成功: user_id={}", user.id);
    ApiResponse::ok(UserResponse {
        id: user.id,
        username: user.username,
        nickname: user.nickname,
        avatar: user.avatar,
        role_code: Some(auth.role_code),
        created_at: user.created_at,
    })
}

/// 用户登出：撤销当前 Token
#[utoipa::path(post, path = "/api/v1/user/logout", tag = "用户",
    security(("bearer" = [])),
    responses((status = 200))
)]
pub async fn logout(
    State(state): State<AppState>,
    _auth: AuthUser,
    headers: HeaderMap,
) -> ApiResult<()> {
    let token = headers
        .get("Authorization")
        .and_then(|v| v.to_str().ok())
        .and_then(|h| h.strip_prefix("Bearer "))
        .ok_or_else(|| AppError::Unauthorized("missing authorization header".into()))?;

    let mut redis = state.redis.clone();
    revoke_token(&mut redis, token).await?;

    tracing::info!("用户登出成功");
    ApiResponse::ok(())
}
