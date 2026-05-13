//! Redis 缓存层
//!
//! 提供 Redis 连接创建、通用 KV 操作，以及 Token 专用的存储/验证/撤销功能。

use anyhow::Context;
use redis::AsyncCommands;
use redis::aio::MultiplexedConnection;
use sha2::{Digest, Sha256};

use crate::error::AppError;

/// 创建 Redis 多路复用连接
pub async fn create_redis_conn(url: &str) -> Result<MultiplexedConnection, AppError> {
    let client = redis::Client::open(url).context("Redis 客户端创建失败")?;
    Ok(client
        .get_multiplexed_async_connection()
        .await
        .context("Redis 连接失败")?)
}

/// 存储带 TTL 的键值对
pub async fn set_ex(
    conn: &mut MultiplexedConnection,
    key: &str,
    value: &str,
    ttl_secs: u64,
) -> Result<(), AppError> {
    Ok(conn
        .set_ex(key, value, ttl_secs)
        .await
        .context("Redis SET EX 失败")?)
}

/// 获取键对应的值
pub async fn get(conn: &mut MultiplexedConnection, key: &str) -> Result<Option<String>, AppError> {
    Ok(conn.get(key).await.context("Redis GET 失败")?)
}

/// 删除键
pub async fn del(conn: &mut MultiplexedConnection, key: &str) -> Result<(), AppError> {
    Ok(conn.del(key).await.context("Redis DEL 失败")?)
}

// ---- Token 专用操作 ----

const TOKEN_PREFIX: &str = "token:";
/// Token 默认 TTL：7 天
const TOKEN_TTL: u64 = 7 * 24 * 60 * 60;

/// 对 Token 做 SHA256 摘要后再作为 Redis key,避免 Redis 备份 / 快照 / RDB
/// 落盘时明文 JWT 泄漏即等同 Token 泄漏。
fn token_key(token: &str) -> String {
    let digest = Sha256::digest(token.as_bytes());
    let hex: String = digest.iter().map(|b| format!("{:02x}", b)).collect();
    format!("{TOKEN_PREFIX}{hex}")
}

/// 保存 Token 到 Redis（TTL 7 天）
pub async fn save_token(
    conn: &mut MultiplexedConnection,
    token: &str,
    user_id: &str,
) -> Result<(), AppError> {
    set_ex(conn, &token_key(token), user_id, TOKEN_TTL).await
}

/// 验证 Token 是否存在于 Redis
pub async fn verify_token(conn: &mut MultiplexedConnection, token: &str) -> Result<bool, AppError> {
    Ok(get(conn, &token_key(token)).await?.is_some())
}

/// 撤销 Token（从 Redis 删除）
pub async fn revoke_token(conn: &mut MultiplexedConnection, token: &str) -> Result<(), AppError> {
    del(conn, &token_key(token)).await
}
