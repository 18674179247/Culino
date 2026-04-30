use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use uuid::Uuid;
use validator::Validate;

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct ShoppingList {
    pub id: Uuid,
    pub user_id: Option<Uuid>,
    pub title: Option<String>,
    pub status: Option<i16>,
    pub created_at: Option<DateTime<Utc>>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct ShoppingListItem {
    pub id: i32,
    pub list_id: Option<Uuid>,
    pub name: String,
    pub amount: Option<String>,
    pub is_checked: Option<bool>,
    pub sort_order: Option<i32>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct MealPlan {
    pub id: Uuid,
    pub user_id: Option<Uuid>,
    pub recipe_id: Option<Uuid>,
    pub plan_date: NaiveDate,
    pub meal_type: i16,
    pub note: Option<String>,
}

// ---- DTOs ----

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct CreateShoppingListReq {
    pub title: Option<String>,
}

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct AddShoppingItemReq {
    pub name: String,
    pub amount: Option<String>,
    pub sort_order: Option<i32>,
}

#[derive(Debug, Deserialize, utoipa::ToSchema)]
pub struct UpdateShoppingItemReq {
    pub name: Option<String>,
    pub amount: Option<String>,
    pub is_checked: Option<bool>,
    pub sort_order: Option<i32>,
}

#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct ShoppingListDetail {
    pub list: ShoppingList,
    pub items: Vec<ShoppingListItem>,
}

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct CreateMealPlanReq {
    pub recipe_id: Option<Uuid>,
    pub plan_date: NaiveDate,
    #[validate(range(min = 1, max = 4, message = "meal_type 范围 1~4"))]
    pub meal_type: i16,
    #[validate(length(max = 200, message = "备注最长 200 个字符"))]
    pub note: Option<String>,
}

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct UpdateMealPlanReq {
    pub recipe_id: Option<Uuid>,
    #[validate(length(max = 200, message = "备注最长 200 个字符"))]
    pub note: Option<String>,
}

#[derive(Debug, Deserialize, utoipa::IntoParams)]
pub struct MealPlanQuery {
    pub start_date: Option<NaiveDate>,
    pub end_date: Option<NaiveDate>,
}
