//! 菜谱模块
//!
//! 提供菜谱的创建、编辑、删除、搜索、随机推荐等功能。
//! 菜谱包含食材、调料、步骤、标签等关联数据。

pub mod handler;
pub mod model;
pub mod repo;
pub mod service;

use axum::{
    Router,
    routing::{get, post},
};
use menu_common::state::AppState;
use utoipa::OpenApi;

#[derive(OpenApi)]
#[openapi(
    paths(
        handler::create,
        handler::get_detail,
        handler::update,
        handler::delete,
        handler::search,
        handler::random,
    ),
    components(schemas(
        model::Recipe,
        model::RecipeIngredient,
        model::RecipeSeasoning,
        model::RecipeStep,
        model::RecipeTag,
        model::CreateRecipeReq,
        model::UpdateRecipeReq,
        model::RecipeIngredientInput,
        model::RecipeSeasoningInput,
        model::RecipeStepInput,
        model::RecipeListItem,
        model::RecipeDetail,
        model::AuthorInfo,
    ))
)]
pub struct RecipeApi;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/", post(handler::create))
        .route("/search", get(handler::search))
        .route("/random", get(handler::random))
        .route(
            "/{id}",
            get(handler::get_detail)
                .put(handler::update)
                .delete(handler::delete),
        )
}
