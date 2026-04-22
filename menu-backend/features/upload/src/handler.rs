//! 上传处理器

use axum::extract::{Multipart, State};
use axum::Json;
use menu_common::auth::AuthUser;
use menu_common::error::AppError;
use menu_common::response::{ApiResponse, ApiResult};
use menu_common::s3;
use menu_common::state::AppState;

use crate::model::{DeleteImageReq, UploadResponse};

const MAX_IMAGE_SIZE: usize = 15 * 1024 * 1024; // 15MB

/// 根据 Content-Type 返回文件扩展名
fn ext_from_content_type(ct: &str) -> Option<&'static str> {
    match ct {
        "image/jpeg" => Some("jpg"),
        "image/png" => Some("png"),
        "image/gif" => Some("gif"),
        "image/webp" => Some("webp"),
        "image/svg+xml" => Some("svg"),
        _ => None,
    }
}

/// 上传图片
#[utoipa::path(
    post,
    path = "/api/v1/upload/image",
    tag = "upload",
    security(("bearer" = [])),
    request_body(content_type = "multipart/form-data", content = inline(UploadImageForm)),
    responses(
        (status = 200, description = "上传成功", body = UploadResponse),
        (status = 400, description = "文件过大或类型不支持"),
    )
)]
pub async fn upload_image(
    State(state): State<AppState>,
    auth: AuthUser,
    mut multipart: Multipart,
) -> ApiResult<UploadResponse> {
    while let Some(field) = multipart
        .next_field()
        .await
        .map_err(|e| AppError::BadRequest(format!("multipart 解析失败: {e}")))?
    {
        let name = field.name().unwrap_or_default().to_string();
        if name != "file" {
            continue;
        }

        let content_type = field
            .content_type()
            .unwrap_or("application/octet-stream")
            .to_string();

        if !content_type.starts_with("image/") {
            return Err(AppError::BadRequest("仅支持图片文件".into()));
        }

        let ext = ext_from_content_type(&content_type).unwrap_or("bin");

        let data = field
            .bytes()
            .await
            .map_err(|e| AppError::BadRequest(format!("读取文件失败: {e}")))?;

        if data.len() > MAX_IMAGE_SIZE {
            return Err(AppError::BadRequest(format!(
                "文件大小超过限制（最大 {}MB）",
                MAX_IMAGE_SIZE / 1024 / 1024
            )));
        }

        let date = chrono::Utc::now().format("%Y-%m-%d");
        let id = uuid::Uuid::new_v4();
        let key = format!("images/{}/{date}/{id}.{ext}", auth.user_id);

        let url = s3::upload_object(&state.s3, &key, &data, &content_type).await?;

        return ApiResponse::ok(UploadResponse { url });
    }

    Err(AppError::BadRequest("缺少 file 字段".into()))
}

/// OpenAPI multipart 表单描述（仅用于文档生成）
#[derive(utoipa::ToSchema)]
#[allow(dead_code)]
struct UploadImageForm {
    /// 图片文件
    #[schema(value_type = String, format = Binary)]
    file: String,
}

/// 删除图片
#[utoipa::path(
    delete,
    path = "/api/v1/upload/image",
    tag = "upload",
    security(("bearer" = [])),
    request_body = DeleteImageReq,
    responses(
        (status = 200, description = "删除成功"),
    )
)]
pub async fn delete_image(
    State(state): State<AppState>,
    auth: AuthUser,
    Json(req): Json<DeleteImageReq>,
) -> ApiResult<()> {
    let prefix = format!("images/{}/", auth.user_id);
    if !req.key.starts_with(&prefix) {
        return Err(AppError::Forbidden("无权删除该文件".into()));
    }

    s3::delete_object(&state.s3, &req.key).await?;

    ApiResponse::ok(())
}
