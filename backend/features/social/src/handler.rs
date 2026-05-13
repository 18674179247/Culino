//! 社交模块 handler
//!
//! 处理收藏菜谱和烹饪记录相关接口，所有操作均需登录。

use crate::model::*;
use crate::repo::cooking_log_repo::{CookingLogRepo, PgCookingLogRepo};
use crate::repo::favorite_repo::{FavoriteRepo, PgFavoriteRepo};
use axum::{
    Json,
    extract::{Path, Query, State},
};
use culino_common::auth::AuthUser;
use culino_common::behavior::spawn_behavior_log;
use culino_common::response::{ApiResponse, ApiResult};
use culino_common::state::AppState;
use uuid::Uuid;
use validator::Validate;

/// 获取当前用户的收藏列表
#[utoipa::path(get, path = "/api/v1/social/favorites", tag = "收藏",
    security(("bearer" = [])),
    responses((status = 200, body = Vec<FavoriteWithTitle>))
)]
pub async fn list_favorites(
    State(state): State<AppState>,
    auth: AuthUser,
) -> ApiResult<Vec<FavoriteWithTitle>> {
    tracing::debug!("查询收藏列表: user_id={}", auth.user_id);
    let repo = PgFavoriteRepo::new(state.pool.clone());
    let rows: Vec<FavoriteWithTitle> = repo.list_by_user(auth.user_id).await?;
    ApiResponse::ok(rows)
}

/// 查询某菜谱是否已被当前用户收藏（避免详情页为判定而拉全量列表）
#[utoipa::path(get, path = "/api/v1/social/favorites/{recipe_id}/check", tag = "收藏",
    security(("bearer" = [])),
    params(("recipe_id" = Uuid, Path, description = "菜谱ID")),
    responses((status = 200, body = bool))
)]
pub async fn check_favorite(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(recipe_id): Path<Uuid>,
) -> ApiResult<bool> {
    let repo = PgFavoriteRepo::new(state.pool.clone());
    let is_fav = repo.is_favorited(auth.user_id, recipe_id).await?;
    ApiResponse::ok(is_fav)
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

/// 点赞/取消点赞
#[utoipa::path(post, path = "/api/v1/social/likes/{recipe_id}", tag = "社交",
    security(("bearer" = [])),
    params(("recipe_id" = Uuid, Path, description = "菜谱ID")),
    responses((status = 200, body = bool, description = "true=已点赞, false=已取消"))
)]
pub async fn toggle_like(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(recipe_id): Path<Uuid>,
) -> ApiResult<bool> {
    let liked = crate::repo::like_repo::LikeRepo::toggle(&state.pool, auth.user_id, recipe_id)
        .await
        ?;
    ApiResponse::ok(liked)
}

/// 获取评论列表
#[utoipa::path(get, path = "/api/v1/social/comments/recipe/{recipe_id}", tag = "社交",
    params(
        ("recipe_id" = Uuid, Path, description = "菜谱ID"),
        CommentListQuery
    ),
    responses((status = 200, body = CommentListResp))
)]
pub async fn list_comments(
    State(state): State<AppState>,
    Path(recipe_id): Path<Uuid>,
    Query(query): Query<CommentListQuery>,
) -> ApiResult<CommentListResp> {
    let pagination = query.pagination();
    let page = pagination.page();
    let page_size = pagination.limit();
    let (comments, total) =
        crate::repo::comment_repo::CommentRepo::list(&state.pool, recipe_id, page, page_size)
            .await
            ?;
    ApiResponse::ok(CommentListResp {
        data: comments,
        total,
        page,
        page_size,
    })
}

/// 发表评论
#[utoipa::path(post, path = "/api/v1/social/comments", tag = "社交",
    security(("bearer" = [])),
    request_body = CreateCommentReq,
    responses((status = 200, body = RecipeComment))
)]
pub async fn create_comment(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(req): Json<CreateCommentReq>,
) -> ApiResult<RecipeComment> {
    req.validate()?;
    let comment = crate::repo::comment_repo::CommentRepo::create(
        &state.pool,
        auth.user_id,
        req.recipe_id,
        &req.content,
    )
    .await
    ?;
    ApiResponse::ok(comment)
}

/// 删除评论（评论作者可删除;管理员可删除任意评论处理违规内容）
#[utoipa::path(delete, path = "/api/v1/social/comments/{id}", tag = "社交",
    security(("bearer" = [])),
    params(("id" = Uuid, Path, description = "评论ID")),
    responses((status = 200, body = bool))
)]
pub async fn delete_comment(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<Uuid>,
) -> ApiResult<bool> {
    let deleted = if auth.is_admin() {
        crate::repo::comment_repo::CommentRepo::delete_as_admin(&state.pool, id)
            .await
            ?
    } else {
        crate::repo::comment_repo::CommentRepo::delete(&state.pool, id, auth.user_id)
            .await
            ?
    };
    ApiResponse::ok(deleted)
}
