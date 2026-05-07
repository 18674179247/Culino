//! 工具模块 handler
//!
//! 处理购物清单和膳食计划相关接口，所有操作均需登录。

use crate::model::*;
use crate::repo::meal_plan_repo::{MealPlanRepo, PgMealPlanRepo};
use crate::repo::shopping_repo::{PgShoppingRepo, ShoppingRepo};
use axum::{
    Json,
    extract::{Path, Query, State},
};
use culino_common::auth::AuthUser;
use culino_common::error::AppError;
use culino_common::response::{ApiResponse, ApiResult};
use culino_common::state::AppState;
use uuid::Uuid;
use validator::Validate;

// ---- 购物清单 ----

/// 获取当前用户的购物清单列表
#[utoipa::path(get, path = "/api/v1/tool/shopping-lists", tag = "购物清单",
    security(("bearer" = [])),
    responses((status = 200, body = Vec<ShoppingList>))
)]
pub async fn list_shopping_lists(
    State(state): State<AppState>,
    auth: AuthUser,
) -> ApiResult<Vec<ShoppingList>> {
    tracing::debug!("查询购物清单列表: user_id={}", auth.user_id);
    let repo = PgShoppingRepo::new(state.pool.clone());
    let rows = repo.list_by_user(auth.user_id).await?;
    ApiResponse::ok(rows)
}

/// 创建购物清单
#[utoipa::path(post, path = "/api/v1/tool/shopping-lists", tag = "购物清单",
    security(("bearer" = [])),
    request_body = CreateShoppingListReq,
    responses((status = 200, body = ShoppingList))
)]
pub async fn create_shopping_list(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(req): Json<CreateShoppingListReq>,
) -> ApiResult<ShoppingList> {
    tracing::info!(
        "创建购物清单: user_id={}, title={:?}",
        auth.user_id,
        req.title
    );
    let repo = PgShoppingRepo::new(state.pool.clone());
    let list = repo.create(auth.user_id, &req).await?;
    tracing::info!("购物清单创建成功: list_id={}", list.id);
    ApiResponse::ok(list)
}

/// 获取购物清单详情（含清单项）
#[utoipa::path(get, path = "/api/v1/tool/shopping-lists/{id}", tag = "购物清单",
    security(("bearer" = [])),
    params(("id" = Uuid, Path, description = "购物清单ID")),
    responses((status = 200, body = ShoppingListDetail))
)]
pub async fn get_shopping_list(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<Uuid>,
) -> ApiResult<ShoppingListDetail> {
    tracing::debug!("查询购物清单详情: list_id={}", id);
    let repo = PgShoppingRepo::new(state.pool.clone());
    let detail = repo
        .find_by_id(id, auth.user_id)
        .await?
        .ok_or_else(|| AppError::NotFound("shopping list not found".into()))?;
    ApiResponse::ok(detail)
}

/// 删除购物清单（级联删除清单项）
#[utoipa::path(delete, path = "/api/v1/tool/shopping-lists/{id}", tag = "购物清单",
    security(("bearer" = [])),
    params(("id" = Uuid, Path, description = "购物清单ID")),
    responses((status = 200, body = Object))
)]
pub async fn delete_shopping_list(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<Uuid>,
) -> ApiResult<bool> {
    tracing::info!("删除购物清单: list_id={}, user_id={}", id, auth.user_id);
    let repo = PgShoppingRepo::new(state.pool.clone());
    repo.delete(id, auth.user_id).await?;
    ApiResponse::ok(true)
}

/// 向购物清单添加商品项
#[utoipa::path(post, path = "/api/v1/tool/shopping-lists/{id}/items", tag = "购物清单",
    security(("bearer" = [])),
    params(("id" = Uuid, Path, description = "购物清单ID")),
    request_body = AddShoppingItemReq,
    responses((status = 200, body = ShoppingListItem))
)]
pub async fn add_shopping_item(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<Uuid>,
    Json(req): Json<AddShoppingItemReq>,
) -> ApiResult<ShoppingListItem> {
    tracing::info!("添加购物项: list_id={}, name={}", id, req.name);
    let repo = PgShoppingRepo::new(state.pool.clone());
    // 验证清单归属权
    repo.find_by_id(id, auth.user_id)
        .await?
        .ok_or_else(|| AppError::NotFound("shopping list not found".into()))?;
    let item = repo.add_item(id, &req).await?;
    ApiResponse::ok(item)
}

/// 更新购物清单项（名称、数量、勾选状态、排序）
#[utoipa::path(put, path = "/api/v1/tool/shopping-lists/{list_id}/items/{item_id}", tag = "购物清单",
    security(("bearer" = [])),
    params(
        ("list_id" = Uuid, Path, description = "购物清单ID"),
        ("item_id" = i32, Path, description = "清单项ID"),
    ),
    request_body = UpdateShoppingItemReq,
    responses((status = 200, body = ShoppingListItem))
)]
pub async fn update_shopping_item(
    State(state): State<AppState>,
    auth: AuthUser,
    Path((list_id, item_id)): Path<(Uuid, i32)>,
    Json(req): Json<UpdateShoppingItemReq>,
) -> ApiResult<ShoppingListItem> {
    tracing::debug!("更新购物项: item_id={}", item_id);
    let repo = PgShoppingRepo::new(state.pool.clone());
    repo.find_by_id(list_id, auth.user_id)
        .await?
        .ok_or_else(|| AppError::NotFound("shopping list not found".into()))?;
    let item = repo.update_item(item_id, list_id, &req).await?;

    if req.is_checked == Some(true) {
        repo.auto_complete_list(list_id).await?;
    }

    ApiResponse::ok(item)
}

/// 删除购物清单项
#[utoipa::path(delete, path = "/api/v1/tool/shopping-lists/{list_id}/items/{item_id}", tag = "购物清单",
    security(("bearer" = [])),
    params(
        ("list_id" = Uuid, Path, description = "购物清单ID"),
        ("item_id" = i32, Path, description = "清单项ID"),
    ),
    responses((status = 200, body = Object))
)]
pub async fn delete_shopping_item(
    State(state): State<AppState>,
    auth: AuthUser,
    Path((list_id, item_id)): Path<(Uuid, i32)>,
) -> ApiResult<bool> {
    tracing::debug!("删除购物项: item_id={}", item_id);
    let repo = PgShoppingRepo::new(state.pool.clone());
    // 验证清单归属权
    repo.find_by_id(list_id, auth.user_id)
        .await?
        .ok_or_else(|| AppError::NotFound("shopping list not found".into()))?;
    repo.delete_item(item_id, list_id).await?;
    ApiResponse::ok(true)
}

// ---- 膳食计划 ----

/// 查询膳食计划（按日期范围，默认未来 7 天）
#[utoipa::path(get, path = "/api/v1/tool/meal-plans", tag = "膳食计划",
    security(("bearer" = [])),
    params(MealPlanQuery),
    responses((status = 200, body = Vec<MealPlan>))
)]
pub async fn list_meal_plans(
    State(state): State<AppState>,
    auth: AuthUser,
    Query(q): Query<MealPlanQuery>,
) -> ApiResult<Vec<MealPlan>> {
    tracing::debug!(
        "查询膳食计划: user_id={}, range={:?}~{:?}",
        auth.user_id,
        q.start_date,
        q.end_date
    );
    let repo = PgMealPlanRepo::new(state.pool.clone());
    let rows = repo
        .list_by_user(auth.user_id, q.start_date, q.end_date)
        .await?;
    ApiResponse::ok(rows)
}

/// 创建膳食计划（同一天同一餐次不可重复）
#[utoipa::path(post, path = "/api/v1/tool/meal-plans", tag = "膳食计划",
    security(("bearer" = [])),
    request_body = CreateMealPlanReq,
    responses((status = 200, body = MealPlan))
)]
pub async fn create_meal_plan(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(req): Json<CreateMealPlanReq>,
) -> ApiResult<MealPlan> {
    tracing::info!(
        "创建膳食计划: user_id={}, date={}, meal_type={}",
        auth.user_id,
        req.plan_date,
        req.meal_type
    );
    req.validate()?;
    let repo = PgMealPlanRepo::new(state.pool.clone());
    let plan = repo.create(auth.user_id, &req).await?;
    tracing::info!("膳食计划创建成功: plan_id={}", plan.id);
    ApiResponse::ok(plan)
}

/// 更新膳食计划
#[utoipa::path(put, path = "/api/v1/tool/meal-plans/{id}", tag = "膳食计划",
    security(("bearer" = [])),
    params(("id" = Uuid, Path, description = "膳食计划ID")),
    request_body = UpdateMealPlanReq,
    responses((status = 200, body = MealPlan))
)]
pub async fn update_meal_plan(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<Uuid>,
    Json(req): Json<UpdateMealPlanReq>,
) -> ApiResult<MealPlan> {
    tracing::info!("更新膳食计划: plan_id={}, user_id={}", id, auth.user_id);
    req.validate()?;
    let repo = PgMealPlanRepo::new(state.pool.clone());
    let plan = repo.update(id, auth.user_id, &req).await?;
    ApiResponse::ok(plan)
}

/// 删除膳食计划
#[utoipa::path(delete, path = "/api/v1/tool/meal-plans/{id}", tag = "膳食计划",
    security(("bearer" = [])),
    params(("id" = Uuid, Path, description = "膳食计划ID")),
    responses((status = 200, body = Object))
)]
pub async fn delete_meal_plan(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<Uuid>,
) -> ApiResult<bool> {
    tracing::info!("删除膳食计划: plan_id={}, user_id={}", id, auth.user_id);
    let repo = PgMealPlanRepo::new(state.pool.clone());
    repo.delete(id, auth.user_id).await?;
    ApiResponse::ok(true)
}

/// 批量添加购物清单项
#[utoipa::path(post, path = "/api/v1/tool/shopping-lists/{id}/items/batch", tag = "购物清单",
    security(("bearer" = [])),
    params(("id" = Uuid, Path, description = "购物清单ID")),
    request_body = BatchAddItemsReq,
    responses((status = 200, body = Vec<ShoppingListItem>))
)]
pub async fn batch_add_shopping_items(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(id): Path<Uuid>,
    Json(req): Json<BatchAddItemsReq>,
) -> ApiResult<Vec<ShoppingListItem>> {
    tracing::info!("批量添加购物项: list_id={}, count={}", id, req.items.len());
    let repo = PgShoppingRepo::new(state.pool.clone());
    repo.find_by_id(id, auth.user_id).await?.ok_or_else(|| {
        culino_common::error::AppError::NotFound("shopping list not found".into())
    })?;
    let items = repo.batch_add_items(id, &req.items).await?;
    ApiResponse::ok(items)
}
