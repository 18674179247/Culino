//! 标签 handler
//!
//! 处理标签的 CRUD 接口，支持按类型筛选。

use crate::model::*;
use crate::repo::tag_repo::{PgTagRepo, TagRepo};
use axum::{
    Json,
    extract::{Path, Query, State},
};
use menu_common::auth::AuthUser;
use menu_common::response::{ApiResponse, ApiResult};
use menu_common::state::AppState;
use serde::Deserialize;

/// 标签查询参数
#[derive(Debug, Deserialize, utoipa::IntoParams)]
pub struct TagQuery {
    /// 标签类型筛选（如 cuisine、diet 等）
    #[serde(rename = "type")]
    pub tag_type: Option<String>,
}

/// 获取标签列表，可按类型筛选
#[utoipa::path(get, path = "/api/v1/ingredient/tags", tag = "标签",
    params(TagQuery),
    responses((status = 200, body = Vec<Tag>))
)]
pub async fn list(State(state): State<AppState>, Query(q): Query<TagQuery>) -> ApiResult<Vec<Tag>> {
    tracing::debug!("查询标签列表: type={:?}", q.tag_type);
    let repo = PgTagRepo::new(state.pool.clone());
    let rows = repo.list(q.tag_type.as_deref()).await?;
    ApiResponse::ok(rows)
}

/// 创建标签
#[utoipa::path(post, path = "/api/v1/ingredient/tags", tag = "标签",
    security(("bearer" = [])),
    request_body = CreateTagReq,
    responses((status = 200, body = Tag))
)]
pub async fn create(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(req): Json<CreateTagReq>,
) -> ApiResult<Tag> {
    auth.require_admin()?;
    tracing::info!("创建标签: name={}, type={}", req.name, req.tag_type);
    let repo = PgTagRepo::new(state.pool.clone());
    let row = repo.create(&req).await?;
    tracing::info!("标签创建成功: id={}", row.id);
    ApiResponse::ok(row)
}

/// 更新标签
#[utoipa::path(put, path = "/api/v1/ingredient/tags/{id}", tag = "标签",
    security(("bearer" = [])),
    params(("id" = i32, Path, description = "标签ID")),
    request_body = UpdateTagReq,
    responses((status = 200, body = Tag))
)]
pub async fn update(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<i32>,
    Json(req): Json<UpdateTagReq>,
) -> ApiResult<Tag> {
    auth.require_admin()?;
    tracing::info!("更新标签: id={}", id);
    let repo = PgTagRepo::new(state.pool.clone());
    let row = repo.update(id, &req).await?;
    ApiResponse::ok(row)
}

/// 删除标签
#[utoipa::path(delete, path = "/api/v1/ingredient/tags/{id}", tag = "标签",
    security(("bearer" = [])),
    params(("id" = i32, Path, description = "标签ID")),
    responses((status = 200, body = Object))
)]
pub async fn remove(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<i32>,
) -> ApiResult<bool> {
    auth.require_admin()?;
    tracing::info!("删除标签: id={}", id);
    let repo = PgTagRepo::new(state.pool.clone());
    repo.delete(id).await?;
    ApiResponse::ok(true)
}
