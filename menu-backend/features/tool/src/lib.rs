//! 工具模块
//!
//! 提供购物清单和膳食计划管理功能。

pub mod handler;
pub mod model;
pub mod repo;

use axum::{
    Router,
    routing::{get, post, put},
};
use menu_common::state::AppState;
use utoipa::OpenApi;

#[derive(OpenApi)]
#[openapi(
    paths(
        handler::list_shopping_lists,
        handler::create_shopping_list,
        handler::get_shopping_list,
        handler::delete_shopping_list,
        handler::add_shopping_item,
        handler::update_shopping_item,
        handler::delete_shopping_item,
        handler::list_meal_plans,
        handler::create_meal_plan,
        handler::update_meal_plan,
        handler::delete_meal_plan,
    ),
    components(schemas(
        model::ShoppingList,
        model::ShoppingListItem,
        model::MealPlan,
        model::CreateShoppingListReq,
        model::AddShoppingItemReq,
        model::UpdateShoppingItemReq,
        model::ShoppingListDetail,
        model::CreateMealPlanReq,
        model::UpdateMealPlanReq,
    ))
)]
pub struct ToolApi;

pub fn routes() -> Router<AppState> {
    Router::new()
        // shopping lists
        .route(
            "/shopping-lists",
            get(handler::list_shopping_lists).post(handler::create_shopping_list),
        )
        .route(
            "/shopping-lists/{id}",
            get(handler::get_shopping_list).delete(handler::delete_shopping_list),
        )
        .route(
            "/shopping-lists/{id}/items",
            post(handler::add_shopping_item),
        )
        .route(
            "/shopping-lists/{list_id}/items/{item_id}",
            put(handler::update_shopping_item).delete(handler::delete_shopping_item),
        )
        // meal plans
        .route(
            "/meal-plans",
            get(handler::list_meal_plans).post(handler::create_meal_plan),
        )
        .route(
            "/meal-plans/{id}",
            put(handler::update_meal_plan).delete(handler::delete_meal_plan),
        )
}
