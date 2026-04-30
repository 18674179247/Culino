use chrono::{DateTime, Utc};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use uuid::Uuid;
use validator::Validate;

// ============================================
// 数据库模型
// ============================================

/// 菜谱营养分析
#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct RecipeNutrition {
    pub recipe_id: Uuid,
    #[schema(value_type = Option<f64>)]
    pub calories: Option<Decimal>,
    #[schema(value_type = Option<f64>)]
    pub protein: Option<Decimal>,
    #[schema(value_type = Option<f64>)]
    pub fat: Option<Decimal>,
    #[schema(value_type = Option<f64>)]
    pub carbohydrate: Option<Decimal>,
    #[schema(value_type = Option<f64>)]
    pub fiber: Option<Decimal>,
    #[schema(value_type = Option<f64>)]
    pub sodium: Option<Decimal>,
    pub analysis_text: Option<String>,
    pub health_score: Option<i16>,
    pub health_tags: Option<Vec<String>>,
    pub suitable_for: Option<Vec<String>>,
    pub cautions: Option<Vec<String>>,
    pub serving_size: Option<String>,
    pub traffic_light: Option<serde_json::Value>,
    pub overall_rating: Option<String>,
    pub summary: Option<String>,
    pub generated_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}

/// 用户偏好画像
#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct UserPreference {
    pub user_id: Uuid,
    pub favorite_cuisines: Option<serde_json::Value>,
    pub favorite_tastes: Option<serde_json::Value>,
    pub favorite_ingredients: Option<serde_json::Value>,
    pub favorite_tags: Option<serde_json::Value>,
    pub dietary_restrictions: Option<Vec<String>>,
    pub health_goals: Option<Vec<String>>,
    pub avg_cooking_time: Option<i32>,
    pub difficulty_preference: Option<i16>,
    pub total_favorites: Option<i32>,
    pub total_cooking_logs: Option<i32>,
    #[schema(value_type = Option<f64>)]
    pub avg_rating: Option<Decimal>,
    pub last_analyzed_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}

/// AI 推荐记录
#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct AiRecommendation {
    pub id: Uuid,
    pub user_id: Option<Uuid>,
    pub recipe_id: Option<Uuid>,
    pub recommendation_type: String,
    #[schema(value_type = f64)]
    pub score: Decimal,
    pub reason: Option<String>,
    pub clicked: Option<bool>,
    pub clicked_at: Option<DateTime<Utc>>,
    pub created_at: Option<DateTime<Utc>>,
}

/// 用户行为日志
#[derive(Debug, FromRow, Serialize)]
pub struct UserBehaviorLog {
    pub id: Uuid,
    pub user_id: Option<Uuid>,
    pub recipe_id: Option<Uuid>,
    pub action_type: String,
    pub action_value: Option<serde_json::Value>,
    pub created_at: Option<DateTime<Utc>>,
}

// ============================================
// DTO 模型
// ============================================

/// 营养分析请求
#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct AnalyzeNutritionReq {
    pub recipe_id: Uuid,
    pub force: Option<bool>,
}

/// 营养分析响应
#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct NutritionAnalysisResp {
    pub recipe_id: Uuid,
    pub nutrition: RecipeNutrition,
    pub is_cached: bool,
}

/// 推荐请求参数
#[derive(Debug, Deserialize, utoipa::IntoParams)]
pub struct RecommendationQuery {
    /// 推荐数量，默认 10
    pub limit: Option<i64>,
    /// 排除已推荐的菜谱
    pub exclude_recommended: Option<bool>,
}

/// 推荐响应项
#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct RecommendationItem {
    pub recipe_id: Uuid,
    pub title: String,
    pub cover_image: Option<String>,
    pub score: f64,
    pub reason: String,
    pub recommendation_type: String,
}

/// 用户偏好分析响应
#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct UserPreferenceResp {
    pub user_id: Uuid,
    pub preference: UserPreference,
    pub top_cuisines: Vec<PreferenceItem>,
    pub top_tastes: Vec<PreferenceItem>,
    pub top_ingredients: Vec<PreferenceItem>,
}

/// 偏好项
#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct PreferenceItem {
    pub name: String,
    pub weight: f64,
}

/// 行为日志创建请求
#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct CreateBehaviorLogReq {
    pub recipe_id: Uuid,
    pub action_type: String,
    pub action_value: Option<serde_json::Value>,
}

/// AI 菜谱识别请求
#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct RecognizeRecipeReq {
    pub image_url: String,
    pub existing_title: Option<String>,
}

/// AI 菜谱识别 - 食材项
#[derive(Debug, Serialize, Deserialize, utoipa::ToSchema)]
pub struct RecognizedIngredient {
    pub name: String,
    pub amount: String,
}

/// AI 菜谱识别响应
#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct RecognizeRecipeResp {
    pub title: String,
    pub description: Option<String>,
    pub difficulty: Option<i16>,
    pub cooking_time: Option<i32>,
    pub servings: Option<i16>,
    pub ingredients: Vec<RecognizedIngredient>,
    pub seasonings: Vec<RecognizedIngredient>,
    pub steps: Vec<String>,
    pub confidence: f64,
}

// ============================================
// 购物清单 AI 解析
// ============================================

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct ParseShoppingTextReq {
    pub text: String,
}

#[derive(Debug, Serialize, Deserialize, utoipa::ToSchema)]
pub struct ParsedShoppingItem {
    pub name: String,
    pub amount: String,
}

#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct ParseShoppingTextResp {
    pub items: Vec<ParsedShoppingItem>,
}
