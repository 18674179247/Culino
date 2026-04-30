//! 统一错误处理
//!
//! 定义 AppError 枚举，自动转换为统一的 ApiResponse 格式。
//! 支持从 sqlx::Error、anyhow::Error、validator::ValidationErrors 自动转换。

use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};

use crate::response::ApiResponse;

/// 应用统一错误类型
#[derive(Debug, thiserror::Error)]
pub enum AppError {
    /// 资源未找到 (404)
    #[error("not found: {0}")]
    NotFound(String),
    /// 未认证 (401)
    #[error("unauthorized: {0}")]
    Unauthorized(String),
    /// 无权限 (403)
    #[error("forbidden: {0}")]
    Forbidden(String),
    /// 请求参数错误 (400)
    #[error("bad request: {0}")]
    BadRequest(String),
    /// 参数校验失败 (422)
    #[error("validation error: {0}")]
    Validation(String),
    /// 资源冲突 (409)，如唯一约束违反
    #[error("conflict: {0}")]
    Conflict(String),
    /// 内部错误 (500)
    #[error(transparent)]
    Internal(#[from] anyhow::Error),
}

impl AppError {
    /// 错误码（语义化字符串，前端可据此做精确处理）
    fn code(&self) -> &'static str {
        match self {
            AppError::NotFound(_) => "NOT_FOUND",
            AppError::Unauthorized(_) => "UNAUTHORIZED",
            AppError::Forbidden(_) => "FORBIDDEN",
            AppError::BadRequest(_) => "BAD_REQUEST",
            AppError::Validation(_) => "VALIDATION_ERROR",
            AppError::Conflict(_) => "CONFLICT",
            AppError::Internal(_) => "INTERNAL_ERROR",
        }
    }

    fn status(&self) -> StatusCode {
        match self {
            AppError::NotFound(_) => StatusCode::NOT_FOUND,
            AppError::Unauthorized(_) => StatusCode::UNAUTHORIZED,
            AppError::Forbidden(_) => StatusCode::FORBIDDEN,
            AppError::BadRequest(_) => StatusCode::BAD_REQUEST,
            AppError::Validation(_) => StatusCode::UNPROCESSABLE_ENTITY,
            AppError::Conflict(_) => StatusCode::CONFLICT,
            AppError::Internal(_) => StatusCode::INTERNAL_SERVER_ERROR,
        }
    }
}

/// 将 sqlx 数据库错误转换为 AppError
impl From<sqlx::Error> for AppError {
    fn from(e: sqlx::Error) -> Self {
        match e {
            sqlx::Error::RowNotFound => AppError::NotFound("resource not found".into()),
            _ => AppError::Internal(e.into()),
        }
    }
}

/// 将 validator 校验错误转换为 AppError
impl From<validator::ValidationErrors> for AppError {
    fn from(e: validator::ValidationErrors) -> Self {
        AppError::Validation(e.to_string())
    }
}

/// 将 AppError 转换为统一的 ApiResponse 格式
impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let status = self.status();
        let message = match &self {
            AppError::Internal(e) => {
                tracing::error!("内部错误: {e:?}");
                "internal server error".to_string()
            }
            other => other.to_string(),
        };
        let body = ApiResponse::<()>::err(self.code(), message);
        (status, body).into_response()
    }
}
