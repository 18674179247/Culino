//! 应用程序入口
//!
//! CLI 子命令采用管道式前置依赖：
//! - check   → 检查配置 + 数据库连接 + Redis 连接 + S3 连接
//! - migrate → check + 执行迁移
//! - serve   → check + migrate + 启动服务（默认）

mod cli;
mod openapi;
mod router;

use clap::Parser;
use menu_common::config::AppConfig;
use menu_common::redis::create_redis_conn;
use menu_common::s3::create_s3_bucket;
use menu_common::state::AppState;
use redis::aio::MultiplexedConnection;
use s3::Bucket;
use sqlx::PgPool;
use tokio::signal;
use tracing_appender::non_blocking::WorkerGuard;
use tracing_subscriber::layer::SubscriberExt;
use tracing_subscriber::util::SubscriberInitExt;

use cli::{Cli, Command};

#[tokio::main]
async fn main() {
    dotenvy::dotenv().ok();

    let cli = Cli::parse();
    let config = AppConfig::from_env();

    let _log_guard = init_tracing(&config);

    let command = cli.command.unwrap_or(Command::Serve);

    match command {
        Command::Check => {
            let (pool, _redis, _s3) = check(&config).await;
            pool.close().await;
            tracing::info!("所有检查通过");
        }
        Command::Migrate => {
            let (pool, _redis, _s3) = check(&config).await;
            migrate(&pool).await;
            pool.close().await;
        }
        Command::Serve => {
            let (pool, redis, s3) = check(&config).await;
            migrate(&pool).await;
            serve(config, pool, redis, s3).await;
        }
    }
}

/// 初始化 tracing：stdout 层 + 按天滚动日志文件层
///
/// 返回 WorkerGuard，调用方需持有直到进程退出以确保日志完整刷写。
fn init_tracing(config: &AppConfig) -> WorkerGuard {
    let env_filter = tracing_subscriber::EnvFilter::try_new(format!(
        "menu_backend={level},menu_common={level},menu_user={level},menu_recipe={level},menu_ingredient={level},menu_social={level},menu_tool={level},menu_upload={level},tower_http=debug",
        level = config.log_level
    ))
    .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("debug"));

    let stdout_layer = tracing_subscriber::fmt::layer().with_target(true);

    let file_appender = tracing_appender::rolling::daily(&config.log_dir, "menu-backend.log");
    let (non_blocking, guard) = tracing_appender::non_blocking(file_appender);

    let file_layer = tracing_subscriber::fmt::layer()
        .with_target(true)
        .with_ansi(false)
        .with_writer(non_blocking);

    tracing_subscriber::registry()
        .with(env_filter)
        .with(stdout_layer)
        .with(file_layer)
        .init();

    guard
}

/// 阶段 1：检查配置、数据库连接、Redis 连接和 S3 连接
async fn check(config: &AppConfig) -> (PgPool, MultiplexedConnection, Box<Bucket>) {
    tracing::info!("运行环境: {:?}", config.run_mode);
    tracing::info!("服务地址: {}", config.server_addr);
    tracing::info!("正在检查数据库连接...");

    let pool = PgPool::connect(&config.database_url)
        .await
        .unwrap_or_else(|e| {
            tracing::error!("数据库连接失败: {e}");
            std::process::exit(1);
        });

    let row: (i64,) = sqlx::query_as("SELECT 1")
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

    // 测试 S3 连通性：上传并删除一个测试对象
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

/// 阶段 2：执行数据库迁移
async fn migrate(pool: &PgPool) {
    tracing::info!("正在执行数据库迁移...");
    sqlx::migrate!()
        .run(pool)
        .await
        .expect("数据库迁移失败");
    tracing::info!("数据库迁移完成");
}

/// 阶段 3：启动 HTTP 服务
async fn serve(config: AppConfig, pool: PgPool, redis: MultiplexedConnection, s3: Box<Bucket>) {
    let addr = config.server_addr.clone();
    let state = AppState { pool, config, redis, s3 };

    let doc = openapi::build_api_doc();
    let app = router::build_router(state, doc);

    let listener = tokio::net::TcpListener::bind(&addr)
        .await
        .expect("端口绑定失败");
    tracing::info!("服务启动于 {}", addr);
    tracing::info!("Swagger UI: http://{}/swagger-ui/", addr);

    axum::serve(listener, app)
        .with_graceful_shutdown(shutdown_signal())
        .await
        .unwrap();

    tracing::info!("服务已关闭");
}

/// 监听系统关闭信号（Ctrl+C 和 SIGTERM）
async fn shutdown_signal() {
    let ctrl_c = async {
        signal::ctrl_c().await.expect("failed to install Ctrl+C handler");
    };

    #[cfg(unix)]
    let terminate = async {
        signal::unix::signal(signal::unix::SignalKind::terminate())
            .expect("failed to install SIGTERM handler")
            .recv()
            .await;
    };

    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        () = ctrl_c => tracing::info!("收到 Ctrl+C 信号，正在关闭..."),
        () = terminate => tracing::info!("收到 SIGTERM 信号，正在关闭..."),
    }
}
