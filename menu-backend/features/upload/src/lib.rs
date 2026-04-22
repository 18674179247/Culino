//! 文件上传模块
//!
//! 提供图片上传到 S3 兼容存储的功能。

pub mod handler;
pub mod model;

use axum::{Router, extract::DefaultBodyLimit, routing::post};
use menu_common::state::AppState;
use utoipa::OpenApi;

#[derive(OpenApi)]
#[openapi(
    paths(
        handler::upload_image,
        handler::delete_image,
    ),
    components(schemas(
        model::UploadResponse,
        model::DeleteImageReq,
    ))
)]
pub struct UploadApi;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/image", post(handler::upload_image).delete(handler::delete_image))
        // 限制请求体大小为 16MB（略大于 handler 中的 15MB 文件限制，留出 multipart 开销）
        .layer(DefaultBodyLimit::max(16 * 1024 * 1024))
}
