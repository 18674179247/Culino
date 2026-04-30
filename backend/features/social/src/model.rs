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

/// 收藏列表项（包含菜谱摘要信息）
#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct FavoriteWithTitle {
    pub user_id: Uuid,
    pub recipe_id: Uuid,
    pub created_at: Option<DateTime<Utc>>,
    pub recipe_title: Option<String>,
    pub cover_image: Option<String>,
    pub difficulty: Option<i16>,
    pub cooking_time: Option<i32>,
    pub servings: Option<i16>,
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

/// 点赞记录
#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct RecipeLike {
    pub user_id: Uuid,
    pub recipe_id: Uuid,
    pub created_at: Option<DateTime<Utc>>,
}

/// 评论（含用户信息）
#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct RecipeComment {
    pub id: Uuid,
    pub recipe_id: Uuid,
    pub user_id: Uuid,
    pub username: String,
    pub nickname: Option<String>,
    pub avatar: Option<String>,
    pub content: String,
    pub created_at: Option<DateTime<Utc>>,
}

/// 创建评论请求
#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct CreateCommentReq {
    pub recipe_id: Uuid,
    #[validate(length(min = 1, max = 1000))]
    pub content: String,
}

/// 评论列表查询参数
#[derive(Debug, Deserialize, utoipa::IntoParams)]
pub struct CommentListQuery {
    pub page: Option<i64>,
    pub page_size: Option<i64>,
}

/// 评论列表响应
#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct CommentListResp {
    pub data: Vec<RecipeComment>,
    pub total: i64,
    pub page: i64,
    pub page_size: i64,
}
