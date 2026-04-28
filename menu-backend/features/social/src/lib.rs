//! 社交模块
//!
//! 提供菜谱收藏和烹饪记录功能。

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
        handler::list_favorites,
        handler::add_favorite,
        handler::remove_favorite,
        handler::list_cooking_logs,
        handler::create_cooking_log,
        handler::update_cooking_log,
        handler::delete_cooking_log,
    ),
    components(schemas(
        model::Favorite,
        model::FavoriteWithTitle,
        model::CookingLog,
        model::CreateCookingLogReq,
        model::UpdateCookingLogReq,
    ))
)]
pub struct SocialApi;

pub fn routes() -> Router<AppState> {
    Router::new()
        // favorites
        .route("/favorites", get(handler::list_favorites))
        .route(
            "/favorites/{recipe_id}",
            post(handler::add_favorite).delete(handler::remove_favorite),
        )
        // cooking logs
        .route(
            "/cooking-logs",
            get(handler::list_cooking_logs).post(handler::create_cooking_log),
        )
        .route(
            "/cooking-logs/{id}",
            put(handler::update_cooking_log).delete(handler::delete_cooking_log),
        )
}
