//! 食材模块
//!
//! 管理食材、调料、标签等配料相关数据的 CRUD 操作。

pub mod handler;
pub mod model;
pub mod repo;

use axum::{
    Router,
    routing::{get, put},
};
use menu_common::state::AppState;
use utoipa::OpenApi;

#[derive(OpenApi)]
#[openapi(
    paths(
        handler::ingredient::list,
        handler::ingredient::get_by_id,
        handler::ingredient::create,
        handler::ingredient::update,
        handler::ingredient::remove,
        handler::ingredient::list_categories,
        handler::seasoning::list,
        handler::seasoning::create,
        handler::seasoning::update,
        handler::seasoning::remove,
        handler::tag::list,
        handler::tag::create,
        handler::tag::update,
        handler::tag::remove,
    ),
    components(schemas(
        model::IngredientCategory,
        model::Ingredient,
        model::Seasoning,
        model::Tag,
        model::CreateIngredientReq,
        model::UpdateIngredientReq,
        model::CreateSeasoningReq,
        model::UpdateSeasoningReq,
        model::CreateTagReq,
        model::UpdateTagReq,
    ))
)]
pub struct IngredientApi;

pub fn routes() -> Router<AppState> {
    Router::new()
        // ingredients
        .route(
            "/ingredients",
            get(handler::ingredient::list).post(handler::ingredient::create),
        )
        .route(
            "/ingredients/{id}",
            get(handler::ingredient::get_by_id)
                .put(handler::ingredient::update)
                .delete(handler::ingredient::remove),
        )
        .route(
            "/ingredient-categories",
            get(handler::ingredient::list_categories),
        )
        // seasonings
        .route(
            "/seasonings",
            get(handler::seasoning::list).post(handler::seasoning::create),
        )
        .route(
            "/seasonings/{id}",
            put(handler::seasoning::update).delete(handler::seasoning::remove),
        )
        // tags
        .route("/tags", get(handler::tag::list).post(handler::tag::create))
        .route(
            "/tags/{id}",
            put(handler::tag::update).delete(handler::tag::remove),
        )
}
