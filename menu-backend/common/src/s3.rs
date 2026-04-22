//! S3 兼容对象存储客户端封装
//!
//! 通过 rust-s3 crate 对接 RustFS/MinIO 等 S3 兼容存储。

use anyhow::Context;
use s3::Bucket;
use s3::Region;
use s3::creds::Credentials;

use crate::config::AppConfig;
use crate::error::AppError;

/// 从应用配置创建 S3 Bucket 客户端
pub fn create_s3_bucket(config: &AppConfig) -> Result<Box<Bucket>, AppError> {
    let region = Region::Custom {
        region: config.s3_region.clone(),
        endpoint: config.s3_endpoint.clone(),
    };

    let credentials = Credentials::new(
        Some(&config.s3_access_key),
        Some(&config.s3_secret_key),
        None,
        None,
        None,
    )
    .context("S3 凭证创建失败")?;

    let bucket = Bucket::new(&config.s3_bucket, region, credentials)
        .context("S3 Bucket 创建失败")?
        .with_path_style();

    Ok(bucket)
}

/// 上传对象到 S3，返回公开访问 URL
pub async fn upload_object(
    bucket: &Bucket,
    key: &str,
    data: &[u8],
    content_type: &str,
) -> Result<String, AppError> {
    bucket
        .put_object_with_content_type(key, data, content_type)
        .await
        .context("S3 上传失败")?;

    Ok(object_url(bucket, key))
}

/// 删除 S3 对象
pub async fn delete_object(bucket: &Bucket, key: &str) -> Result<(), AppError> {
    bucket
        .delete_object(key)
        .await
        .context("S3 删除失败")?;
    Ok(())
}

/// 拼接对象的公开访问 URL
///
/// path-style 时 `bucket.url()` 已包含 bucket 名称，如 `http://localhost:9000/menu`
pub fn object_url(bucket: &Bucket, key: &str) -> String {
    format!("{}/{}", bucket.url(), key)
}
