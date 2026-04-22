use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use uuid::Uuid;
use validator::Validate;

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct User {
    pub id: Uuid,
    pub username: String,
    pub nickname: Option<String>,
    #[serde(skip_serializing)]
    #[schema(read_only)]
    pub password_hash: String,
    pub avatar: Option<String>,
    pub role_id: Option<i32>,
    pub is_active: Option<bool>,
    pub created_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct Role {
    pub id: i32,
    pub code: String,
    pub name: String,
    pub description: Option<String>,
}

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct Permission {
    pub id: i32,
    pub code: String,
    pub name: String,
    pub module: String,
}

#[derive(Debug)]
pub struct CreateUser {
    pub username: String,
    pub nickname: Option<String>,
    pub password_hash: String,
}

#[derive(Debug)]
pub struct UpdateUser {
    pub nickname: Option<String>,
    pub avatar: Option<String>,
}

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct LoginReq {
    #[validate(length(min = 2, message = "用户名至少 2 个字符"))]
    pub username: String,
    #[validate(length(min = 6, max = 128, message = "密码长度 6~128 个字符"))]
    pub password: String,
}

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct RegisterReq {
    #[validate(length(min = 2, message = "用户名至少 2 个字符"))]
    pub username: String,
    #[validate(length(min = 6, max = 128, message = "密码长度 6~128 个字符"))]
    pub password: String,
    pub nickname: Option<String>,
}

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct UpdateProfileReq {
    #[validate(length(max = 50, message = "昵称最长 50 个字符"))]
    pub nickname: Option<String>,
    #[validate(length(max = 500, message = "头像 URL 最长 500 个字符"))]
    pub avatar: Option<String>,
}

#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct UserResponse {
    pub id: Uuid,
    pub username: String,
    pub nickname: Option<String>,
    pub avatar: Option<String>,
    pub role_code: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Serialize, utoipa::ToSchema)]
pub struct TokenResponse {
    pub token: String,
    pub user: UserResponse,
}
