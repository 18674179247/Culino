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

/// 若配置了 BOOTSTRAP_ADMIN_USERNAME，则将该用户的 role_id 提升为 admin。
/// 仅当用户已存在时生效，不存在则只记录一条 warn。
pub async fn bootstrap_admin(pool: &PgPool, config: &AppConfig) {
    let Some(ref username) = config.bootstrap_admin_username else {
        return;
    };

    let admin_role: Option<(i32,)> = sqlx::query_as("SELECT id FROM roles WHERE code = 'admin'")
        .fetch_optional(pool)
        .await
        .expect("查询 admin 角色失败");

    let Some((admin_role_id,)) = admin_role else {
        tracing::warn!("roles 表缺少 'admin' 角色，跳过自动提权");
        return;
    };

    let updated = sqlx::query(
        "UPDATE users SET role_id = $1 WHERE username = $2 AND role_id IS DISTINCT FROM $1",
    )
    .bind(admin_role_id)
    .bind(username)
    .execute(pool)
    .await
    .expect("提权 admin 失败");

    if updated.rows_affected() > 0 {
        tracing::info!("已将用户 {} 提权为 admin", username);
    } else {
        let exists: Option<(uuid::Uuid,)> =
            sqlx::query_as("SELECT id FROM users WHERE username = $1")
                .bind(username)
                .fetch_optional(pool)
                .await
                .expect("查询用户失败");
        if exists.is_none() {
            tracing::warn!(
                "BOOTSTRAP_ADMIN_USERNAME={} 尚未注册，请先注册后重启应用以自动提权",
                username
            );
        } else {
            tracing::debug!("用户 {} 已经是 admin", username);
        }
    }
}
