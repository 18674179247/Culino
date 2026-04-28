use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use uuid::Uuid;
use validator::Validate;

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct Favorite {
    pub user_id: Uuid,
    pub recipe_id: Uuid,
    pub created_at: Option<DateTime<Utc>>,
}

/// 收藏列表项（包含菜谱标题）
#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct FavoriteWithTitle {
    pub user_id: Uuid,
    pub recipe_id: Uuid,
    pub created_at: Option<DateTime<Utc>>,
    pub recipe_title: Option<String>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct CookingLog {
    pub id: Uuid,
    pub recipe_id: Option<Uuid>,
    pub user_id: Option<Uuid>,
    pub rating: Option<i16>,
    pub note: Option<String>,
    pub cooked_at: Option<NaiveDate>,
    pub created_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct CreateCookingLogReq {
    pub recipe_id: Uuid,
    #[validate(range(min = 1, max = 5, message = "评分范围 1~5"))]
    pub rating: Option<i16>,
    #[validate(length(max = 2000, message = "备注最长 2000 个字符"))]
    pub note: Option<String>,
    pub cooked_at: Option<NaiveDate>,
}

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct UpdateCookingLogReq {
    #[validate(range(min = 1, max = 5, message = "评分范围 1~5"))]
    pub rating: Option<i16>,
    #[validate(length(max = 2000, message = "备注最长 2000 个字符"))]
    pub note: Option<String>,
}
