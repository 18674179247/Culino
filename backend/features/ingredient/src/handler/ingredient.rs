//! 食材 handler
//!
//! 处理食材和食材分类的 CRUD 接口。

use crate::model::*;
use crate::repo::ingredient_repo::{IngredientRepo, PgIngredientRepo};
use axum::{
    Json,
    extract::{Path, State},
};
use culino_common::auth::AdminUser;
use culino_common::error::AppError;
use culino_common::response::{ApiResponse, ApiResult};
use culino_common::state::AppState;

/// 获取所有食材列表
#[utoipa::path(get, path = "/api/v1/ingredient/ingredients", tag = "食材",
    responses((status = 200, body = Vec<Ingredient>))
)]
pub async fn list(State(state): State<AppState>) -> ApiResult<Vec<Ingredient>> {
    tracing::debug!("查询食材列表");
    let repo = PgIngredientRepo::new(state.pool.clone());
    let rows = repo.list().await?;
    ApiResponse::ok(rows)
}

/// 根据 ID 获取食材详情
#[utoipa::path(get, path = "/api/v1/ingredient/ingredients/{id}", tag = "食材",
    params(("id" = i32, Path, description = "食材ID")),
    responses((status = 200, body = Ingredient))
)]
pub async fn get_by_id(
    State(state): State<AppState>,
    Path(id): Path<i32>,
) -> ApiResult<Ingredient> {
    tracing::debug!("查询食材详情: id={}", id);
    let repo = PgIngredientRepo::new(state.pool.clone());
    let row = repo
        .find_by_id(id)
        .await?
        .ok_or_else(|| AppError::NotFound("ingredient not found".into()))?;
    ApiResponse::ok(row)
}

/// 创建食材
#[utoipa::path(post, path = "/api/v1/ingredient/ingredients", tag = "食材",
    security(("bearer" = [])),
    request_body = CreateIngredientReq,
    responses((status = 200, body = Ingredient))
)]
pub async fn create(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(req): Json<CreateIngredientReq>,
) -> ApiResult<Ingredient> {
    tracing::info!("创建食材: name={}", req.name);
    let repo = PgIngredientRepo::new(state.pool.clone());
    let row = repo.create(&req).await?;
    tracing::info!("食材创建成功: id={}", row.id);
    ApiResponse::ok(row)
}

/// 更新食材
#[utoipa::path(put, path = "/api/v1/ingredient/ingredients/{id}", tag = "食材",
    security(("bearer" = [])),
    params(("id" = i32, Path, description = "食材ID")),
    request_body = UpdateIngredientReq,
    responses((status = 200, body = Ingredient))
)]
pub async fn update(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<i32>,
    Json(req): Json<UpdateIngredientReq>,
) -> ApiResult<Ingredient> {
    tracing::info!("更新食材: id={}", id);
    let repo = PgIngredientRepo::new(state.pool.clone());
    let row = repo.update(id, &req).await?;
    ApiResponse::ok(row)
}

/// 删除食材
#[utoipa::path(delete, path = "/api/v1/ingredient/ingredients/{id}", tag = "食材",
    security(("bearer" = [])),
    params(("id" = i32, Path, description = "食材ID")),
    responses((status = 200, body = Object))
)]
pub async fn remove(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<i32>,
) -> ApiResult<bool> {
    tracing::info!("删除食材: id={}", id);
    let repo = PgIngredientRepo::new(state.pool.clone());
    repo.delete(id).await?;
    ApiResponse::ok(true)
}

/// 获取所有食材分类
#[utoipa::path(get, path = "/api/v1/ingredient/ingredient-categories", tag = "食材",
    responses((status = 200, body = Vec<IngredientCategory>))
)]
pub async fn list_categories(State(state): State<AppState>) -> ApiResult<Vec<IngredientCategory>> {
    tracing::debug!("查询食材分类列表");
    let repo = PgIngredientRepo::new(state.pool.clone());
    let rows = repo.list_categories().await?;
    ApiResponse::ok(rows)
}
