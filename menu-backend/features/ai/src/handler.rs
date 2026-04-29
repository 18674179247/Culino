use axum::{
    Json,
    extract::{Path, Query, State},
};
use uuid::Uuid;

use menu_common::auth::AuthUser;
use menu_common::response::{ApiResponse, ApiResult};
use menu_common::state::AppState;

use crate::model::*;
use crate::nutrition::NutritionService;
use crate::preference::PreferenceService;
use crate::recommendation::RecommendationService;
use crate::repo::AiRepo;

// ============================================
// 营养分析相关
// ============================================

/// 触发菜谱营养分析
#[utoipa::path(post, path = "/api/v1/ai/nutrition/analyze/{recipe_id}", tag = "AI",
    security(("bearer" = [])),
    params(("recipe_id" = Uuid, Path, description = "菜谱ID")),
    responses((status = 200, body = NutritionAnalysisResp))
)]
pub async fn analyze_nutrition(
    State(state): State<AppState>,
    Path(recipe_id): Path<Uuid>,
) -> ApiResult<NutritionAnalysisResp> {
    tracing::info!("Analyzing nutrition for recipe {}", recipe_id);

    let api_key = state.config.deepseek_api_key.clone().ok_or_else(|| {
        menu_common::error::AppError::Internal(anyhow::anyhow!("DeepSeek API key not configured"))
    })?;

    let service = NutritionService::new(state.pool.clone(), api_key)
        .map_err(|e| menu_common::error::AppError::Internal(e))?;

    let nutrition = service
        .analyze_recipe_nutrition(recipe_id, false)
        .await
        .map_err(|e| menu_common::error::AppError::Internal(e))?;

    ApiResponse::ok(NutritionAnalysisResp {
        recipe_id,
        nutrition,
        is_cached: false,
    })
}

/// 获取菜谱营养信息
#[utoipa::path(get, path = "/api/v1/ai/nutrition/{recipe_id}", tag = "AI",
    params(("recipe_id" = Uuid, Path, description = "菜谱ID")),
    responses((status = 200, body = RecipeNutrition))
)]
pub async fn get_nutrition(
    State(state): State<AppState>,
    Path(recipe_id): Path<Uuid>,
) -> ApiResult<RecipeNutrition> {
    tracing::debug!("Getting nutrition for recipe {}", recipe_id);

    let repo = AiRepo::new(state.pool.clone());
    let nutrition = repo
        .get_nutrition(recipe_id)
        .await
        .map_err(|e| menu_common::error::AppError::Internal(e))?
        .ok_or_else(|| menu_common::error::AppError::NotFound("Nutrition data not found".into()))?;

    ApiResponse::ok(nutrition)
}

// ============================================
// 推荐系统相关
// ============================================

/// 个性化推荐
#[utoipa::path(get, path = "/api/v1/ai/recommend/personalized", tag = "AI",
    security(("bearer" = [])),
    params(RecommendationQuery),
    responses((status = 200, body = Vec<RecommendationItem>))
)]
pub async fn personalized_recommendations(
    State(state): State<AppState>,
    auth: AuthUser,
    Query(query): Query<RecommendationQuery>,
) -> ApiResult<Vec<RecommendationItem>> {
    tracing::info!(
        "Getting personalized recommendations for user {}",
        auth.user_id
    );

    let service = RecommendationService::new(state.pool.clone());
    let limit = query.limit.unwrap_or(10).min(50);

    let recommendations = service
        .personalized_recommendations(auth.user_id, limit)
        .await
        .map_err(|e| menu_common::error::AppError::Internal(e))?;

    ApiResponse::ok(recommendations)
}

/// 相似菜谱推荐
#[utoipa::path(get, path = "/api/v1/ai/recommend/similar/{recipe_id}", tag = "AI",
    params(
        ("recipe_id" = Uuid, Path, description = "菜谱ID"),
        RecommendationQuery
    ),
    responses((status = 200, body = Vec<RecommendationItem>))
)]
pub async fn similar_recommendations(
    State(state): State<AppState>,
    Path(recipe_id): Path<Uuid>,
    Query(query): Query<RecommendationQuery>,
) -> ApiResult<Vec<RecommendationItem>> {
    tracing::info!("Getting similar recommendations for recipe {}", recipe_id);

    let service = RecommendationService::new(state.pool.clone());
    let limit = query.limit.unwrap_or(10).min(50);

    let recommendations = service
        .similar_recommendations(recipe_id, limit)
        .await
        .map_err(|e| menu_common::error::AppError::Internal(e))?;

    ApiResponse::ok(recommendations)
}

/// 热门推荐
#[utoipa::path(get, path = "/api/v1/ai/recommend/trending", tag = "AI",
    params(RecommendationQuery),
    responses((status = 200, body = Vec<RecommendationItem>))
)]
pub async fn trending_recommendations(
    State(state): State<AppState>,
    Query(query): Query<RecommendationQuery>,
) -> ApiResult<Vec<RecommendationItem>> {
    tracing::info!("Getting trending recommendations");

    let service = RecommendationService::new(state.pool.clone());
    let limit = query.limit.unwrap_or(10).min(50);

    let recommendations = service
        .trending_recommendations(limit)
        .await
        .map_err(|e| menu_common::error::AppError::Internal(e))?;

    ApiResponse::ok(recommendations)
}

/// 基于健康目标的推荐
#[utoipa::path(get, path = "/api/v1/ai/recommend/health/{goal}", tag = "AI",
    security(("bearer" = [])),
    params(
        ("goal" = String, Path, description = "健康目标：减脂/增肌/保持健康"),
        RecommendationQuery
    ),
    responses((status = 200, body = Vec<RecommendationItem>))
)]
pub async fn health_goal_recommendations(
    State(state): State<AppState>,
    auth: AuthUser,
    Path(goal): Path<String>,
    Query(query): Query<RecommendationQuery>,
) -> ApiResult<Vec<RecommendationItem>> {
    tracing::info!("Getting health goal recommendations: {}", goal);

    let service = RecommendationService::new(state.pool.clone());
    let limit = query.limit.unwrap_or(10).min(50);

    let recommendations = service
        .health_goal_recommendations(auth.user_id, &goal, limit)
        .await
        .map_err(|e| menu_common::error::AppError::Internal(e))?;

    ApiResponse::ok(recommendations)
}

// ============================================
// 用户偏好相关
// ============================================

/// 分析用户偏好
#[utoipa::path(post, path = "/api/v1/ai/preference/analyze", tag = "AI",
    security(("bearer" = [])),
    responses((status = 200, body = UserPreference))
)]
pub async fn analyze_preference(
    State(state): State<AppState>,
    auth: AuthUser,
) -> ApiResult<UserPreference> {
    tracing::info!("Analyzing preference for user {}", auth.user_id);

    let service = PreferenceService::new(state.pool.clone());

    let preference = service
        .analyze_user_preference(auth.user_id)
        .await
        .map_err(|e| menu_common::error::AppError::Internal(e))?;

    ApiResponse::ok(preference)
}

/// 获取用户偏好画像
#[utoipa::path(get, path = "/api/v1/ai/preference/profile", tag = "AI",
    security(("bearer" = [])),
    responses((status = 200, body = UserPreference))
)]
pub async fn get_preference_profile(
    State(state): State<AppState>,
    auth: AuthUser,
) -> ApiResult<UserPreference> {
    tracing::debug!("Getting preference profile for user {}", auth.user_id);

    let service = PreferenceService::new(state.pool.clone());

    let preference = service
        .get_user_preference(auth.user_id)
        .await
        .map_err(|e| menu_common::error::AppError::Internal(e))?
        .ok_or_else(|| {
            menu_common::error::AppError::NotFound("User preference not found".into())
        })?;

    ApiResponse::ok(preference)
}

// ============================================
// 行为日志相关
// ============================================

/// 记录用户行为
#[utoipa::path(post, path = "/api/v1/ai/behavior/log", tag = "AI",
    security(("bearer" = [])),
    request_body = CreateBehaviorLogReq,
    responses((status = 200, body = bool))
)]
pub async fn log_behavior(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(req): Json<CreateBehaviorLogReq>,
) -> ApiResult<bool> {
    tracing::debug!(
        "Logging behavior: user={}, recipe={}, action={}",
        auth.user_id,
        req.recipe_id,
        req.action_type
    );

    let repo = AiRepo::new(state.pool.clone());

    repo.log_user_behavior(
        auth.user_id,
        req.recipe_id,
        &req.action_type,
        req.action_value,
    )
    .await
    .map_err(|e| menu_common::error::AppError::Internal(e))?;

    ApiResponse::ok(true)
}

// ============================================
// AI 菜谱识别相关
// ============================================

/// AI 识别菜谱
#[utoipa::path(post, path = "/api/v1/ai/recipe/recognize", tag = "AI",
    security(("bearer" = [])),
    request_body = RecognizeRecipeReq,
    responses((status = 200, body = RecognizeRecipeResp))
)]
pub async fn recognize_recipe(
    State(state): State<AppState>,
    Json(req): Json<RecognizeRecipeReq>,
) -> ApiResult<RecognizeRecipeResp> {
    tracing::info!("Recognizing recipe from image: {}", req.image_url);

    let api_key = state.config.deepseek_api_key.clone().ok_or_else(|| {
        menu_common::error::AppError::Internal(anyhow::anyhow!("DeepSeek API key not configured"))
    })?;

    let service = crate::recognition::RecognitionService::new(api_key)
        .map_err(|e| menu_common::error::AppError::Internal(e))?;

    let result = service
        .recognize_from_image(&req.image_url, req.existing_title.as_deref())
        .await
        .map_err(|e| menu_common::error::AppError::Internal(e))?;

    ApiResponse::ok(result)
}
