//! 社交模块 handler
//!
//! 处理收藏菜谱和烹饪记录相关接口，所有操作均需登录。

use crate::model::*;
use crate::repo::cooking_log_repo::{CookingLogRepo, PgCookingLogRepo};
use crate::repo::favorite_repo::{FavoriteRepo, PgFavoriteRepo};
use axum::{
    Json,
    extract::{Path, State},
};
use menu_common::auth::AuthUser;
use menu_common::response::{ApiResponse, ApiResult};
use menu_common::state::AppState;
use uuid::Uuid;
use validator::Validate;

/// 异步记录用户行为（fire-and-forget）
fn spawn_behavior_log(
    state: &AppState,
    user_id: Uuid,
    recipe_id: Uuid,
    action: &'static str,
    value: Option<serde_json::Value>,
) {
    if let Some(logger) = state.behavior_logger.clone() {
        tokio::spawn(async move {
            if let Err(e) = logger.log(user_id, recipe_id, action, value).await {
                tracing::error!(
                    "行为日志记录失败: user={user_id}, recipe={recipe_id}, action={action}, error={e}"
                );
            }
        });
    }
}

/// 获取当前用户的收藏列表
#[utoipa::path(get, path = "/api/v1/social/favorites", tag = "收藏",
    security(("bearer" = [])),
    responses((status = 200, body = Vec<Favorite>))
)]
pub async fn list_favorites(
    State(state): State<AppState>,
    auth: AuthUser,
) -> ApiResult<Vec<Favorite>> {
    tracing::debug!("查询收藏列表: user_id={}", auth.user_id);
    let repo = PgFavoriteRepo::new(state.pool.clone());
    let rows = repo.list_by_user(auth.user_id).await?;
    ApiResponse::ok(rows)
}

/// 收藏菜谱（重复收藏不会报错，返回已有记录）
#[utoipa::path(post, path = "/api/v1/social/favorites/{recipe_id}", tag = "收藏",
    security(("bearer" = [])),
    params(("recipe_id" = Uuid, Path, description = "菜谱ID")),
    responses((status = 200, body = Favorite))
)]
pub async fn add_favorite(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(recipe_id): Path<Uuid>,
) -> ApiResult<Favorite> {
    tracing::info!(
        "添加收藏: user_id={}, recipe_id={}",
        auth.user_id,
        recipe_id
    );
    let repo = PgFavoriteRepo::new(state.pool.clone());
    let fav = repo.add(auth.user_id, recipe_id).await?;

    spawn_behavior_log(&state, auth.user_id, recipe_id, "favorite", None);

    ApiResponse::ok(fav)
}

/// 取消收藏菜谱
#[utoipa::path(delete, path = "/api/v1/social/favorites/{recipe_id}", tag = "收藏",
    security(("bearer" = [])),
    params(("recipe_id" = Uuid, Path, description = "菜谱ID")),
    responses((status = 200, body = Object))
)]
pub async fn remove_favorite(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(recipe_id): Path<Uuid>,
) -> ApiResult<bool> {
    tracing::info!(
        "取消收藏: user_id={}, recipe_id={}",
        auth.user_id,
        recipe_id
    );
    let repo = PgFavoriteRepo::new(state.pool.clone());
    repo.remove(auth.user_id, recipe_id).await?;

    spawn_behavior_log(&state, auth.user_id, recipe_id, "unfavorite", None);

    ApiResponse::ok(true)
}

/// 获取当前用户的烹饪记录列表
#[utoipa::path(get, path = "/api/v1/social/cooking-logs", tag = "烹饪记录",
    security(("bearer" = [])),
    responses((status = 200, body = Vec<CookingLog>))
)]
pub async fn list_cooking_logs(
    State(state): State<AppState>,
    auth: AuthUser,
) -> ApiResult<Vec<CookingLog>> {
    tracing::debug!("查询烹饪记录: user_id={}", auth.user_id);
    let repo = PgCookingLogRepo::new(state.pool.clone());
    let rows = repo.list_by_user(auth.user_id).await?;
    ApiResponse::ok(rows)
}

/// 创建烹饪记录
#[utoipa::path(post, path = "/api/v1/social/cooking-logs", tag = "烹饪记录",
    security(("bearer" = [])),
    request_body = CreateCookingLogReq,
    responses((status = 200, body = CookingLog))
)]
pub async fn create_cooking_log(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(req): Json<CreateCookingLogReq>,
) -> ApiResult<CookingLog> {
    tracing::info!(
        "创建烹饪记录: user_id={}, recipe_id={}",
        auth.user_id,
        req.recipe_id
    );
    req.validate()?;
    let repo = PgCookingLogRepo::new(state.pool.clone());
    let log = repo.create(auth.user_id, &req).await?;
    tracing::info!("烹饪记录创建成功: log_id={}", log.id);

    let action_value = req.rating.map(|r| serde_json::json!({"rating": r}));
    spawn_behavior_log(&state, auth.user_id, req.recipe_id, "cook", action_value);

    ApiResponse::ok(log)
}

/// 更新烹饪记录（评分、备注）
#[utoipa::path(put, path = "/api/v1/social/cooking-logs/{id}", tag = "烹饪记录",
    security(("bearer" = [])),
    params(("id" = Uuid, Path, description = "烹饪记录ID")),
    request_body = UpdateCookingLogReq,
    responses((status = 200, body = CookingLog))
)]
pub async fn update_cooking_log(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<Uuid>,
    Json(req): Json<UpdateCookingLogReq>,
) -> ApiResult<CookingLog> {
    tracing::info!("更新烹饪记录: log_id={}, user_id={}", id, auth.user_id);
    req.validate()?;
    let repo = PgCookingLogRepo::new(state.pool.clone());
    let log = repo.update(id, auth.user_id, &req).await?;
    ApiResponse::ok(log)
}

/// 删除烹饪记录
#[utoipa::path(delete, path = "/api/v1/social/cooking-logs/{id}", tag = "烹饪记录",
    security(("bearer" = [])),
    params(("id" = Uuid, Path, description = "烹饪记录ID")),
    responses((status = 200, body = Object))
)]
pub async fn delete_cooking_log(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<Uuid>,
) -> ApiResult<bool> {
    tracing::info!("删除烹饪记录: log_id={}, user_id={}", id, auth.user_id);
    let repo = PgCookingLogRepo::new(state.pool.clone());
    repo.delete(id, auth.user_id).await?;
    ApiResponse::ok(true)
}
