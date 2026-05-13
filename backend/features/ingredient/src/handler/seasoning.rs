//! 调料 handler
//!
//! 处理调料的 CRUD 接口。

use crate::model::*;
use crate::repo::seasoning_repo::{PgSeasoningRepo, SeasoningRepo};
use axum::{
    Json,
    extract::{Path, State},
};
use culino_common::auth::AdminUser;
use culino_common::response::{ApiResponse, ApiResult};
use culino_common::state::AppState;

/// 获取所有调料列表
#[utoipa::path(get, path = "/api/v1/ingredient/seasonings", tag = "调料",
    responses((status = 200, body = Vec<Seasoning>))
)]
pub async fn list(State(state): State<AppState>) -> ApiResult<Vec<Seasoning>> {
    tracing::debug!("查询调料列表");
    let repo = PgSeasoningRepo::new(state.pool.clone());
    let rows = repo.list().await?;
    ApiResponse::ok(rows)
}

/// 创建调料
#[utoipa::path(post, path = "/api/v1/ingredient/seasonings", tag = "调料",
    security(("bearer" = [])),
    request_body = CreateSeasoningReq,
    responses((status = 200, body = Seasoning))
)]
pub async fn create(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(req): Json<CreateSeasoningReq>,
) -> ApiResult<Seasoning> {

    tracing::info!("创建调料: name={}", req.name);
    let repo = PgSeasoningRepo::new(state.pool.clone());
    let row = repo.create(&req).await?;
    tracing::info!("调料创建成功: id={}", row.id);
    ApiResponse::ok(row)
}

/// 更新调料
#[utoipa::path(put, path = "/api/v1/ingredient/seasonings/{id}", tag = "调料",
    security(("bearer" = [])),
    params(("id" = i32, Path, description = "调料ID")),
    request_body = UpdateSeasoningReq,
    responses((status = 200, body = Seasoning))
)]
pub async fn update(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<i32>,
    Json(req): Json<UpdateSeasoningReq>,
) -> ApiResult<Seasoning> {

    tracing::info!("更新调料: id={}", id);
    let repo = PgSeasoningRepo::new(state.pool.clone());
    let row = repo.update(id, &req).await?;
    ApiResponse::ok(row)
}

/// 删除调料
#[utoipa::path(delete, path = "/api/v1/ingredient/seasonings/{id}", tag = "调料",
    security(("bearer" = [])),
    params(("id" = i32, Path, description = "调料ID")),
    responses((status = 200, body = Object))
)]
pub async fn remove(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<i32>,
) -> ApiResult<bool> {

    tracing::info!("删除调料: id={}", id);
    let repo = PgSeasoningRepo::new(state.pool.clone());
    repo.delete(id).await?;
    ApiResponse::ok(true)
}
