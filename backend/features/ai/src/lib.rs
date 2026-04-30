//! AI 功能模块
//!
//! 提供以下功能：
//! - 菜谱营养分析（基于 DeepSeek API）
//! - 用户偏好分析
//! - 智能推荐系统（个性化、相似、热门、健康目标）
//! - 用户行为日志记录

pub mod deepseek;
pub mod handler;
pub mod model;
pub mod nutrition;
pub mod preference;
pub mod recognition;
pub mod recommendation;
pub mod repo;

pub use handler::*;
pub use model::*;

use utoipa::OpenApi;

/// AI 模块 OpenAPI 文档
#[derive(OpenApi)]
#[openapi(
    paths(
        handler::analyze_nutrition,
        handler::get_nutrition,
        handler::personalized_recommendations,
        handler::similar_recommendations,
        handler::trending_recommendations,
        handler::health_goal_recommendations,
        handler::analyze_preference,
        handler::get_preference_profile,
        handler::log_behavior,
        handler::recognize_recipe,
        handler::parse_shopping_text,
    ),
    components(schemas(
        RecipeNutrition,
        UserPreference,
        AiRecommendation,
        NutritionAnalysisResp,
        RecommendationItem,
        UserPreferenceResp,
        PreferenceItem,
        CreateBehaviorLogReq,
        RecognizeRecipeReq,
        RecognizeRecipeResp,
        RecognizedIngredient,
        ParseShoppingTextReq,
        ParseShoppingTextResp,
        ParsedShoppingItem,
    )),
    tags(
        (name = "AI", description = "AI 功能接口")
    )
)]
pub struct AiApi;
