use culino_common::config::AppConfig;
use culino_common::state::AppState;
use redis::aio::MultiplexedConnection;
use s3::Bucket;
use sqlx::PgPool;
use tokio::signal;

/// 启动 HTTP 服务
pub async fn serve(config: AppConfig, pool: PgPool, redis: MultiplexedConnection, s3: Box<Bucket>) {
    let addr = config.server_addr.clone();

    let behavior_logger = if config.deepseek_api_key.is_some() {
        let ai_repo = culino_ai::repo::AiRepo::new(pool.clone());
        Some(ai_repo.into_behavior_logger())
    } else {
        None
    };

    let state = AppState {
        pool,
        config,
        redis,
        s3,
        behavior_logger,
    };

    let doc = crate::openapi::build_api_doc();
    let app = crate::router::build_router(state, doc);

    let listener = tokio::net::TcpListener::bind(&addr)
        .await
        .expect("端口绑定失败");

    crate::banner::print_banner(&addr);

    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<std::net::SocketAddr>(),
    )
    .with_graceful_shutdown(shutdown_signal())
    .await
    .unwrap();

    tracing::info!("服务已关闭");
}

async fn shutdown_signal() {
    let ctrl_c = async {
        signal::ctrl_c()
            .await
            .expect("failed to install Ctrl+C handler");
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
