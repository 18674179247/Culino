//! 统一响应格式
//!
//! 成功和失败都使用同一个 `ApiResponse<T>` 结构：
//! - 成功：`{ "ok": true, "data": ..., "error": null }`
//! - 失败：`{ "ok": false, "data": null, "error": { "code": "NOT_FOUND", "message": "..." } }`

use axum::response::{IntoResponse, Response};
use axum::Json;
use serde::Serialize;

use crate::error::AppError;

/// 统一 API 响应
#[derive(Debug, Serialize)]
pub struct ApiResponse<T: Serialize> {
    pub ok: bool,
    pub data: Option<T>,
    pub error: Option<ErrorDetail>,
}

/// 错误详情
#[derive(Debug, Serialize)]
pub struct ErrorDetail {
    pub code: &'static str,
    pub message: String,
}

impl<T: Serialize> ApiResponse<T> {
    /// 构造成功响应
    pub fn ok(data: T) -> ApiResult<T> {
        Ok(Self {
            ok: true,
            data: Some(data),
            error: None,
        })
    }

    /// 构造错误响应
    pub fn err(code: &'static str, message: impl Into<String>) -> Self {
        Self {
            ok: false,
            data: None,
            error: Some(ErrorDetail {
                code,
                message: message.into(),
            }),
        }
    }
}

// ---- IntoResponse ----

impl<T: Serialize> IntoResponse for ApiResponse<T> {
    fn into_response(self) -> Response {
        Json(self).into_response()
    }
}

/// handler 返回类型别名
pub type ApiResult<T> = Result<ApiResponse<T>, AppError>;
