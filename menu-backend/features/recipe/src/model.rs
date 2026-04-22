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
    #[schema(value_type = Option<f64>)]
    pub amount: Option<rust_decimal::Decimal>,
    pub unit: Option<String>,
    pub note: Option<String>,
    pub sort_order: Option<i32>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct RecipeSeasoning {
    pub id: i32,
    pub recipe_id: Option<Uuid>,
    pub seasoning_id: Option<i32>,
    #[schema(value_type = Option<f64>)]
    pub amount: Option<rust_decimal::Decimal>,
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
    pub ingredient_id: i32,
    pub amount: Option<f64>,
    pub unit: Option<String>,
    pub note: Option<String>,
    pub sort_order: Option<i32>,
}

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct RecipeSeasoningInput {
    pub seasoning_id: i32,
    pub amount: Option<f64>,
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
    pub page: Option<i64>,
    pub page_size: Option<i64>,
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
}
