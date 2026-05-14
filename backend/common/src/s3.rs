//! S3 兼容对象存储客户端封装
//!
//! 通过 rust-s3 crate 对接 RustFS/MinIO 等 S3 兼容存储。

use anyhow::Context;
use s3::Bucket;
use s3::Region;
use s3::creds::Credentials;

use crate::config::AppConfig;
use crate::error::AppError;

/// 公开访问 URL 前缀（启动时由 main 写入，避免每次访问都从配置读）
static mut S3_PUBLIC_BASE: String = String::new();

pub fn set_public_base(base: &str) {
    // SAFETY: 仅在启动时调用一次,运行期不变
    #[allow(static_mut_refs)]
    unsafe {
        S3_PUBLIC_BASE = base.trim_end_matches('/').to_string();
    }
}

fn public_base() -> &'static str {
    #[allow(static_mut_refs)]
    unsafe {
        &S3_PUBLIC_BASE
    }
}

/// 从应用配置创建 S3 Bucket 客户端
pub fn create_s3_bucket(config: &AppConfig) -> Result<Box<Bucket>, AppError> {
    set_public_base(&format!(
        "{}/{}",
        config.s3_public_endpoint.trim_end_matches('/'),
        config.s3_bucket
    ));

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
    let resp = bucket
        .put_object_with_content_type(key, data, content_type)
        .await
        .context("S3 上传失败")?;

    if !(200..300).contains(&resp.status_code()) {
        let body = String::from_utf8_lossy(resp.bytes());
        return Err(AppError::Internal(anyhow::anyhow!(
            "S3 上传失败 status={} body={}",
            resp.status_code(),
            body
        )));
    }

    Ok(object_url(bucket, key))
}

/// 删除 S3 对象
pub async fn delete_object(bucket: &Bucket, key: &str) -> Result<(), AppError> {
    bucket.delete_object(key).await.context("S3 删除失败")?;
    Ok(())
}

/// 拼接对象的公开访问 URL
///
/// 优先使用 `S3_PUBLIC_ENDPOINT` 配置的客户端可访问地址，
/// 而非 `S3_ENDPOINT`（可能是 Docker 内网地址，客户端访问不到）。
pub fn object_url(_bucket: &Bucket, key: &str) -> String {
    let base = public_base();
    if base.is_empty() {
        // fallback：如果没设置（不该发生），降级到 bucket.url()
        return format!("{}/{}", _bucket.url(), key);
    }
    format!("{base}/{key}")
}
