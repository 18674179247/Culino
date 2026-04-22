//! 全局应用状态
//!
//! 在 Axum 路由间共享数据库连接池、Redis 连接和配置。

use crate::config::AppConfig;
use redis::aio::MultiplexedConnection;
use s3::Bucket;
use sqlx::PgPool;

/// 应用共享状态，通过 Axum 的 State 提取器注入到 handler 中
#[derive(Clone)]
pub struct AppState {
    /// PostgreSQL 连接池
    pub pool: PgPool,
    /// 应用配置
    pub config: AppConfig,
    /// Redis 多路复用连接
    pub redis: MultiplexedConnection,
    /// S3 兼容对象存储客户端
    pub s3: Box<Bucket>,
}
