use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use uuid::Uuid;
use validator::Validate;

// ---- DB models ----

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct Recipe {
    pub id: Uuid,
    pub title: String,
    pub description: Option<String>,
    pub cover_image: Option<String>,
    pub difficulty: Option<i16>,
    pub cooking_time: Option<i32>,
    pub prep_time: Option<i32>,
    pub servings: Option<i16>,
    pub source: Option<String>,
    pub author_id: Option<Uuid>,
    pub status: Option<i16>,
    pub created_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct RecipeIngredient {
    pub id: i32,
    pub recipe_id: Option<Uuid>,
    pub ingredient_id: Option<i32>,
    pub ingredient_name: String,
    pub amount: Option<String>,
    pub unit: Option<String>,
    pub note: Option<String>,
    pub sort_order: Option<i32>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct RecipeSeasoning {
    pub id: i32,
    pub recipe_id: Option<Uuid>,
    pub seasoning_id: Option<i32>,
    pub seasoning_name: String,
    pub amount: Option<String>,
    pub unit: Option<String>,
    pub sort_order: Option<i32>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct RecipeStep {
    pub id: i32,
    pub recipe_id: Option<Uuid>,
    pub step_number: i32,
    pub content: String,
    pub image: Option<String>,
    pub duration: Option<i32>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct RecipeTag {
    pub recipe_id: Uuid,
    pub tag_id: i32,
    pub tag_name: String,
}

// ---- DTOs ----

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct CreateRecipeReq {
    #[validate(length(min = 1, max = 200, message = "标题长度 1~200 个字符"))]
    pub title: String,
    #[validate(length(max = 2000, message = "描述最长 2000 个字符"))]
    pub description: Option<String>,
    pub cover_image: Option<String>,
    #[validate(range(min = 1, max = 5, message = "难度范围 1~5"))]
    pub difficulty: Option<i16>,
    #[validate(range(min = 1, message = "烹饪时间必须为正数"))]
    pub cooking_time: Option<i32>,
    #[validate(range(min = 0, message = "准备时间不能为负数"))]
    pub prep_time: Option<i32>,
    #[validate(range(min = 1, max = 100, message = "份数范围 1~100"))]
    pub servings: Option<i16>,
    pub source: Option<String>,
    pub ingredients: Option<Vec<RecipeIngredientInput>>,
    pub seasonings: Option<Vec<RecipeSeasoningInput>>,
    pub steps: Option<Vec<RecipeStepInput>>,
    pub tag_ids: Option<Vec<i32>>,
}

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct UpdateRecipeReq {
    #[validate(length(min = 1, max = 200, message = "标题长度 1~200 个字符"))]
    pub title: Option<String>,
    #[validate(length(max = 2000, message = "描述最长 2000 个字符"))]
    pub description: Option<String>,
    pub cover_image: Option<String>,
    #[validate(range(min = 1, max = 5, message = "难度范围 1~5"))]
    pub difficulty: Option<i16>,
    #[validate(range(min = 1, message = "烹饪时间必须为正数"))]
    pub cooking_time: Option<i32>,
    #[validate(range(min = 0, message = "准备时间不能为负数"))]
    pub prep_time: Option<i32>,
    #[validate(range(min = 1, max = 100, message = "份数范围 1~100"))]
    pub servings: Option<i16>,
    pub source: Option<String>,
    pub ingredients: Option<Vec<RecipeIngredientInput>>,
    pub seasonings: Option<Vec<RecipeSeasoningInput>>,
    pub steps: Option<Vec<RecipeStepInput>>,
    pub tag_ids: Option<Vec<i32>>,
}

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct RecipeIngredientInput {
    pub ingredient_id: Option<i32>,
    pub name: Option<String>,
    pub amount: Option<String>,
    pub unit: Option<String>,
    pub note: Option<String>,
    pub sort_order: Option<i32>,
}

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct RecipeSeasoningInput {
    pub seasoning_id: Option<i32>,
    pub name: Option<String>,
    pub amount: Option<String>,
    pub unit: Option<String>,
    pub sort_order: Option<i32>,
}

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct RecipeStepInput {
    pub step_number: i32,
    pub content: String,
    pub image: Option<String>,
    pub duration: Option<i32>,
}

#[derive(Debug, Deserialize, utoipa::IntoParams)]
pub struct RecipeSearchParams {
    pub keyword: Option<String>,
    pub difficulty: Option<i16>,
    pub max_cooking_time: Option<i32>,
    pub tag_ids: Option<String>,
    pub ingredient_ids: Option<String>,
    pub author_id: Option<Uuid>,
    /// 页码,从 1 开始,默认 1,上限 10000
    pub page: Option<i64>,
    /// 每页条数,默认 20,范围 [1,100]
    pub page_size: Option<i64>,
}

impl RecipeSearchParams {
    /// 从字段构造 PaginationParams,复用 common 的 clamp 规则
    pub fn pagination(&self) -> culino_common::pagination::PaginationParams {
        culino_common::pagination::PaginationParams {
            page: self.page,
            page_size: self.page_size,
        }
    }
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct RecipeListItem {
    pub id: Uuid,
    pub title: String,
    pub description: Option<String>,
    pub cover_image: Option<String>,
    pub difficulty: Option<i16>,
    pub cooking_time: Option<i32>,
    pub servings: Option<i16>,
    pub author_id: Option<Uuid>,
    pub created_at: Option<DateTime<Utc>>,
}

/// 带 total_count 的列表项，用于单次查询分页
#[derive(Debug, FromRow)]
pub struct RecipeListItemCounted {
    pub id: Uuid,
    pub title: String,
    pub description: Option<String>,
    pub cover_image: Option<String>,
    pub difficulty: Option<i16>,
    pub cooking_time: Option<i32>,
    pub servings: Option<i16>,
    pub author_id: Option<Uuid>,
    pub created_at: Option<DateTime<Utc>>,
    pub total_count: i64,
}

impl RecipeListItemCounted {
    pub fn into_item(self) -> RecipeListItem {
        RecipeListItem {
            id: self.id,
            title: self.title,
            description: self.description,
            cover_image: self.cover_image,
            difficulty: self.difficulty,
            cooking_time: self.cooking_time,
            servings: self.servings,
            author_id: self.author_id,
            created_at: self.created_at,
        }
    }
}

#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct RecipeDetail {
    pub recipe: Recipe,
    pub ingredients: Vec<RecipeIngredient>,
    pub seasonings: Vec<RecipeSeasoning>,
    pub steps: Vec<RecipeStep>,
    pub tags: Vec<RecipeTag>,
    /// 营养信息（可能为空，如果尚未分析）
    #[serde(skip_serializing_if = "Option::is_none")]
    pub nutrition: Option<RecipeNutritionInfo>,
    /// 作者信息
    #[serde(skip_serializing_if = "Option::is_none")]
    pub author: Option<AuthorInfo>,
    /// 点赞数
    #[serde(skip_serializing_if = "Option::is_none")]
    pub like_count: Option<i64>,
    /// 评论数
    #[serde(skip_serializing_if = "Option::is_none")]
    pub comment_count: Option<i64>,
}

/// 菜谱营养信息（简化版，用于详情展示）
#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct RecipeNutritionInfo {
    pub calories: Option<f64>,
    pub protein: Option<f64>,
    pub fat: Option<f64>,
    pub carbohydrate: Option<f64>,
    pub fiber: Option<f64>,
    pub sodium: Option<f64>,
    pub health_score: Option<i16>,
    pub health_tags: Option<Vec<String>>,
    pub suitable_for: Option<Vec<String>>,
    pub analysis_text: Option<String>,
    pub serving_size: Option<String>,
    pub traffic_light: Option<serde_json::Value>,
    pub overall_rating: Option<String>,
    pub summary: Option<String>,
    pub cautions: Option<Vec<String>>,
}

/// 菜谱作者信息
#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct AuthorInfo {
    pub id: Uuid,
    pub username: String,
    pub nickname: Option<String>,
    pub avatar: Option<String>,
}
