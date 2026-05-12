use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use uuid::Uuid;
use validator::{Validate, ValidationError};

/// 密码强度校验：8-18 位，必须同时包含字母和数字
pub fn validate_password_strength(pwd: &str) -> Result<(), ValidationError> {
    let len = pwd.chars().count();
    if !(8..=18).contains(&len) {
        return Err(
            ValidationError::new("password_length").with_message("密码长度必须为 8-18 位".into())
        );
    }
    let has_letter = pwd.chars().any(|c| c.is_ascii_alphabetic());
    let has_digit = pwd.chars().any(|c| c.is_ascii_digit());
    if !has_letter || !has_digit {
        return Err(ValidationError::new("password_composition")
            .with_message("密码必须同时包含字母和数字".into()));
    }
    Ok(())
}

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
    pub invited_by: Option<String>,
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
    pub invited_by: String,
}

#[derive(Debug)]
pub struct UpdateUser {
    pub nickname: Option<String>,
    pub avatar: Option<String>,
}

// -----------------------------
// 邀请码
// -----------------------------

#[derive(Debug, FromRow, Serialize, utoipa::ToSchema)]
pub struct InviteCode {
    pub code: String,
    pub created_by: Option<Uuid>,
    pub max_uses: i32,
    pub used_count: i32,
    pub expires_at: Option<DateTime<Utc>>,
    pub note: Option<String>,
    pub created_at: DateTime<Utc>,
}

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct CreateInviteCodeReq {
    /// 最大可用次数，默认 1 次
    #[validate(range(min = 1, max = 1000, message = "最大使用次数必须在 1-1000 之间"))]
    pub max_uses: Option<i32>,
    /// 过期时间（ISO8601），为空表示永不过期
    pub expires_at: Option<DateTime<Utc>>,
    /// 备注（如邀请对象）
    #[validate(length(max = 200, message = "备注最长 200 个字符"))]
    pub note: Option<String>,
}

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct LoginReq {
    #[validate(length(min = 2, max = 50, message = "用户名长度 2~50 个字符"))]
    pub username: String,
    #[validate(length(min = 1, max = 128, message = "密码不能为空"))]
    pub password: String,
}

#[derive(Debug, Deserialize, Validate, utoipa::ToSchema)]
pub struct RegisterReq {
    #[validate(length(min = 2, max = 50, message = "用户名长度 2~50 个字符"))]
    pub username: String,
    #[validate(custom(function = "validate_password_strength"))]
    pub password: String,
    #[validate(length(max = 50, message = "昵称最长 50 个字符"))]
    pub nickname: Option<String>,
    /// 邀请码（必填，由管理员生成后分发）
    #[validate(length(min = 4, max = 64, message = "邀请码长度不合法"))]
    pub invite_code: String,
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
