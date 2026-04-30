use culino_common::config::AppConfig;
use culino_common::redis::create_redis_conn;
use culino_common::s3::create_s3_bucket;
use redis::aio::MultiplexedConnection;
use s3::Bucket;
use sqlx::PgPool;

/// 检查配置、数据库连接、Redis 连接和 S3 连接
pub async fn check(config: &AppConfig) -> (PgPool, MultiplexedConnection, Box<Bucket>) {
    tracing::info!("运行环境: {:?}", config.run_mode);
    tracing::info!("服务地址: {}", config.server_addr);
    tracing::info!("正在检查数据库连接...");

    let pool = PgPool::connect(&config.database_url)
        .await
        .unwrap_or_else(|e| {
            tracing::error!("数据库连接失败: {e}");
            std::process::exit(1);
        });

    let row: (i32,) = sqlx::query_as("SELECT 1")
        .fetch_one(&pool)
        .await
        .expect("数据库查询失败");
    assert_eq!(row.0, 1);
    tracing::info!("数据库连接正常");

    tracing::info!("正在检查 Redis 连接...");
    let redis = create_redis_conn(&config.redis_url)
        .await
        .unwrap_or_else(|e| {
            tracing::error!("Redis 连接失败: {e}");
            std::process::exit(1);
        });
    tracing::info!("Redis 连接正常");

    tracing::info!("正在检查 S3 连接...");
    let s3 = create_s3_bucket(config).unwrap_or_else(|e| {
        tracing::error!("S3 客户端创建失败: {e}");
        std::process::exit(1);
    });

    let test_key = format!("_health_check_{}", uuid::Uuid::new_v4());
    s3.put_object_with_content_type(&test_key, b"ok", "text/plain")
        .await
        .unwrap_or_else(|e| {
            tracing::error!("S3 连接测试失败: {e}");
            std::process::exit(1);
        });
    s3.delete_object(&test_key).await.unwrap_or_else(|e| {
        tracing::error!("S3 删除测试对象失败: {e}");
        std::process::exit(1);
    });
    tracing::info!("S3 连接正常");

    (pool, redis, s3)
}

/// 执行数据库迁移
pub async fn migrate(pool: &PgPool) {
    tracing::info!("正在执行数据库迁移...");
    sqlx::migrate!().run(pool).await.expect("数据库迁移失败");
    tracing::info!("数据库迁移完成");
}
