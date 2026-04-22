//! 上传相关数据模型

use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// 上传成功响应
#[derive(Debug, Serialize, ToSchema)]
pub struct UploadResponse {
    /// 上传后的文件访问 URL
    pub url: String,
}

/// 删除图片请求
#[derive(Debug, Deserialize, ToSchema)]
pub struct DeleteImageReq {
    /// 对象存储中的 key，如 `images/2024-01-01/xxx.jpg`
    pub key: String,
}
