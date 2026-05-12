//! 用户模块 handler
//!
//! 处理用户注册、登录、登出、获取个人信息、更新个人资料、邀请码管理等接口。

use axum::extract::Path;
use axum::http::HeaderMap;
use axum::{Json, extract::State};

use culino_common::auth::{AuthUser, encode_jwt, hash_password, verify_password};
use culino_common::error::AppError;
use culino_common::redis::{revoke_token, save_token};
use culino_common::response::{ApiResponse, ApiResult};
use culino_common::state::AppState;
use validator::Validate;

use crate::model::*;
use crate::repo::{InviteCodeRepo, PgInviteCodeRepo, PgUserRepo, UserRepo};

/// 生成邀请码：UUID 去掉连字符后取前 16 位，简短好记且碰撞概率极低
fn generate_invite_code() -> String {
    uuid::Uuid::new_v4().simple().to_string()[..16].to_string()
}

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
    let invite_code = req.invite_code.clone();

    let user = repo
        .create_with_invite(&CreateUser {
            username: req.username,
            nickname: req.nickname,
            password_hash,
            invited_by: invite_code,
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

    let user = repo.find_by_username(&req.username).await?.ok_or_else(|| {
        tracing::warn!("登录失败: username={}", req.username);
        AppError::Unauthorized("invalid username or password".into())
    })?;

    if !verify_password(&req.password, &user.password_hash)? {
        tracing::warn!("登录失败: username={}", req.username);
        return Err(AppError::Unauthorized(
            "invalid username or password".into(),
        ));
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
pub async fn me(State(state): State<AppState>, auth: AuthUser) -> ApiResult<UserResponse> {
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

// -----------------------------
// 邀请码管理（仅 admin 可访问）
// -----------------------------

/// 管理员创建邀请码
#[utoipa::path(post, path = "/api/v1/user/invite-codes", tag = "邀请码",
    security(("bearer" = [])),
    request_body = CreateInviteCodeReq,
    responses((status = 200, body = InviteCode))
)]
pub async fn create_invite_code(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(req): Json<CreateInviteCodeReq>,
) -> ApiResult<InviteCode> {
    auth.require_admin()?;
    req.validate()?;

    let repo = PgInviteCodeRepo::new(state.pool.clone());
    let code = generate_invite_code();
    let max_uses = req.max_uses.unwrap_or(1);
    let invite = repo
        .create(
            &code,
            auth.user_id,
            max_uses,
            req.expires_at,
            req.note.as_deref(),
        )
        .await?;

    tracing::info!(
        "邀请码创建: code={}, created_by={}, max_uses={}",
        invite.code,
        auth.user_id,
        max_uses
    );
    ApiResponse::ok(invite)
}

/// 管理员查看所有邀请码
#[utoipa::path(get, path = "/api/v1/user/invite-codes", tag = "邀请码",
    security(("bearer" = [])),
    responses((status = 200, body = Vec<InviteCode>))
)]
pub async fn list_invite_codes(
    State(state): State<AppState>,
    auth: AuthUser,
) -> ApiResult<Vec<InviteCode>> {
    auth.require_admin()?;
    let repo = PgInviteCodeRepo::new(state.pool.clone());
    let codes = repo.list().await?;
    ApiResponse::ok(codes)
}

/// 管理员吊销邀请码
#[utoipa::path(delete, path = "/api/v1/user/invite-codes/{code}", tag = "邀请码",
    security(("bearer" = [])),
    params(("code" = String, Path, description = "邀请码")),
    responses((status = 200, body = Object))
)]
pub async fn revoke_invite_code(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(code): Path<String>,
) -> ApiResult<bool> {
    auth.require_admin()?;
    let repo = PgInviteCodeRepo::new(state.pool.clone());
    repo.revoke(&code).await?;
    tracing::info!("邀请码已吊销: code={}, by={}", code, auth.user_id);
    ApiResponse::ok(true)
}

/// 查询当前用户统计信息(菜谱数 / 收藏数 / 烹饪记录数)
/// 替代前端用 list_favorites + list_cooking_logs + list_recipes 拉全量再 count 的反模式
#[utoipa::path(get, path = "/api/v1/user/me/stats", tag = "用户",
    security(("bearer" = [])),
    responses((status = 200, body = UserStats))
)]
pub async fn me_stats(
    State(state): State<AppState>,
    auth: AuthUser,
) -> ApiResult<UserStats> {
    // 单行子查询,三个 COUNT 共用一次往返
    let stats: (i64, i64, i64) = sqlx::query_as(
        r#"
        SELECT
            (SELECT COUNT(*) FROM recipes WHERE author_id = $1 AND status = 1),
            (SELECT COUNT(*) FROM favorites WHERE user_id = $1),
            (SELECT COUNT(*) FROM cooking_logs WHERE user_id = $1)
        "#,
    )
    .bind(auth.user_id)
    .fetch_one(&state.pool)
    .await?;
    ApiResponse::ok(UserStats {
        recipe_count: stats.0,
        favorite_count: stats.1,
        cooking_log_count: stats.2,
    })
}
