//! 菜谱模块 handler
//!
//! 处理菜谱的创建、查看详情、更新、删除、搜索、随机推荐等接口。

use crate::model::*;
use crate::service::RecipeService;
use axum::{
    extract::{Path, Query, State},
    Json,
};
use culino_common::auth::AuthUser;
use culino_common::pagination::PaginatedResponse;
use culino_common::response::{ApiResponse, ApiResult};
use culino_common::state::AppState;
use serde::Deserialize;
use uuid::Uuid;

/// 创建菜谱（需要登录）
#[utoipa::path(post, path = "/api/v1/recipe", tag = "菜谱",
    security(("bearer" = [])),
    request_body = CreateRecipeReq,
    responses((status = 200, body = RecipeDetail))
)]
pub async fn create(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(req): Json<CreateRecipeReq>,
) -> ApiResult<RecipeDetail> {
    tracing::info!("创建菜谱: user_id={}, title={}", auth.user_id, req.title);
    let svc = RecipeService::new(state.pool.clone());
    let detail = svc.create(auth.user_id, &req).await?;
    tracing::info!("菜谱创建成功: recipe_id={}", detail.recipe.id);

    // 异步触发营养分析
    let pool = state.pool.clone();
    let recipe_id = detail.recipe.id;
    let api_key = state.config.deepseek_api_key.clone();

    tokio::spawn(async move {
        if let Some(key) = api_key {
            tracing::info!("开始异步分析菜谱营养: recipe_id={}", recipe_id);
            match culino_ai::nutrition::NutritionService::new(pool, key) {
                Ok(ai_svc) => match ai_svc.analyze_recipe_nutrition(recipe_id, false).await {
                    Ok(_) => tracing::info!("菜谱营养分析完成: recipe_id={}", recipe_id),
                    Err(e) => {
                        tracing::error!("菜谱营养分析失败: recipe_id={}, error={}", recipe_id, e)
                    }
                },
                Err(e) => tracing::error!("创建营养分析服务失败: {}", e),
            }
        } else {
            tracing::debug!("未配置 DeepSeek API Key，跳过营养分析");
        }
    });

    ApiResponse::ok(detail)
}

/// 获取菜谱详情（含食材、调料、步骤、标签、营养信息）
#[utoipa::path(get, path = "/api/v1/recipe/{id}", tag = "菜谱",
    params(("id" = Uuid, Path, description = "菜谱ID")),
    responses((status = 200, body = RecipeDetail))
)]
pub async fn get_detail(
    State(state): State<AppState>,
    Path(id): Path<Uuid>,
) -> ApiResult<RecipeDetail> {
    tracing::debug!("查询菜谱详情: recipe_id={}", id);
    let svc = RecipeService::new(state.pool.clone());
    let detail = svc.get_detail(id).await?;

    // 注意：这里不记录浏览行为，因为无法获取可选的用户信息
    // 如果需要记录，可以创建一个单独的端点或使用前端调用行为日志 API

    ApiResponse::ok(detail)
}

/// 更新菜谱（需要登录，仅作者可操作）
#[utoipa::path(put, path = "/api/v1/recipe/{id}", tag = "菜谱",
    security(("bearer" = [])),
    params(("id" = Uuid, Path, description = "菜谱ID")),
    request_body = UpdateRecipeReq,
    responses((status = 200, body = RecipeDetail))
)]
pub async fn update(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<Uuid>,
    Json(req): Json<UpdateRecipeReq>,
) -> ApiResult<RecipeDetail> {
    tracing::info!("更新菜谱: recipe_id={}, user_id={}", id, auth.user_id);
    let svc = RecipeService::new(state.pool.clone());
    let detail = svc.update(id, auth.user_id, &req).await?;
    tracing::info!("菜谱更新成功: recipe_id={}", id);
    ApiResponse::ok(detail)
}

/// 删除菜谱（需要登录，仅作者可操作）
#[utoipa::path(delete, path = "/api/v1/recipe/{id}", tag = "菜谱",
    security(("bearer" = [])),
    params(("id" = Uuid, Path, description = "菜谱ID")),
    responses((status = 200, body = Object))
)]
pub async fn delete(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<Uuid>,
) -> ApiResult<bool> {
    tracing::info!("删除菜谱: recipe_id={}, user_id={}", id, auth.user_id);
    let svc = RecipeService::new(state.pool.clone());
    svc.delete(id, auth.user_id).await?;
    tracing::info!("菜谱删除成功: recipe_id={}", id);
    ApiResponse::ok(true)
}

/// 搜索菜谱（支持关键词、难度、烹饪时间、标签、食材等筛选条件）
#[utoipa::path(get, path = "/api/v1/recipe/search", tag = "菜谱",
    params(RecipeSearchParams),
    responses((status = 200, body = PaginatedResponse<RecipeListItem>))
)]
pub async fn search(
    State(state): State<AppState>,
    Query(params): Query<RecipeSearchParams>,
) -> ApiResult<PaginatedResponse<RecipeListItem>> {
    tracing::debug!(
        "搜索菜谱: keyword={:?}, difficulty={:?}",
        params.keyword,
        params.difficulty
    );
    let svc = RecipeService::new(state.pool.clone());
    let page = params.page.unwrap_or(1).max(1);
    let page_size = params.page_size.unwrap_or(20).clamp(1, 100);
    let (data, total) = svc.search(&params).await?;
    tracing::debug!("搜索结果: total={}, page={}", total, page);
    ApiResponse::ok(PaginatedResponse {
        data,
        total,
        page,
        page_size,
    })
}

/// 随机推荐查询参数
#[derive(Debug, Deserialize, utoipa::IntoParams)]
pub struct RandomQuery {
    /// 返回数量，默认 5，最大 20
    pub count: Option<i64>,
}

/// 随机推荐菜谱
#[utoipa::path(get, path = "/api/v1/recipe/random", tag = "菜谱",
    params(RandomQuery),
    responses((status = 200, body = Vec<RecipeListItem>))
)]
pub async fn random(
    State(state): State<AppState>,
    Query(q): Query<RandomQuery>,
) -> ApiResult<Vec<RecipeListItem>> {
    tracing::debug!("随机推荐菜谱: count={:?}", q.count);
    let svc = RecipeService::new(state.pool.clone());
    let items = svc.random(q.count).await?;
    ApiResponse::ok(items)
}
