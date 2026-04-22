use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;

// ---- DB models ----

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct IngredientCategory {
    pub id: i32,
    pub name: String,
    pub sort_order: Option<i32>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct Ingredient {
    pub id: i32,
    pub name: String,
    pub category_id: Option<i32>,
    pub unit: Option<String>,
    pub image: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct Seasoning {
    pub id: i32,
    pub name: String,
    pub unit: Option<String>,
    pub image: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct Tag {
    pub id: i32,
    pub name: String,
    #[sqlx(rename = "type")]
    #[serde(rename = "type")]
    pub tag_type: String,
    pub color: Option<String>,
    pub sort_order: Option<i32>,
}

// ---- DTOs ----

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct CreateIngredientReq {
    pub name: String,
    pub category_id: Option<i32>,
    pub unit: Option<String>,
    pub image: Option<String>,
}

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct UpdateIngredientReq {
    pub name: Option<String>,
    pub category_id: Option<i32>,
    pub unit: Option<String>,
    pub image: Option<String>,
}

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct CreateSeasoningReq {
    pub name: String,
    pub unit: Option<String>,
    pub image: Option<String>,
}

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct UpdateSeasoningReq {
    pub name: Option<String>,
    pub unit: Option<String>,
    pub image: Option<String>,
}

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct CreateTagReq {
    pub name: String,
    #[serde(rename = "type")]
    pub tag_type: String,
    pub color: Option<String>,
    pub sort_order: Option<i32>,
}

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct UpdateTagReq {
    pub name: Option<String>,
    #[serde(rename = "type")]
    pub tag_type: Option<String>,
    pub color: Option<String>,
    pub sort_order: Option<i32>,
}
