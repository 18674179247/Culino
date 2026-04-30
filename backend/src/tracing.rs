use culino_common::config::AppConfig;
use tracing_appender::non_blocking::WorkerGuard;
use tracing_subscriber::layer::SubscriberExt;
use tracing_subscriber::util::SubscriberInitExt;

/// 初始化 tracing：stdout 层 + 按天滚动日志文件层
///
/// 返回 WorkerGuard，调用方需持有直到进程退出以确保日志完整刷写。
pub fn init_tracing(config: &AppConfig) -> WorkerGuard {
    let env_filter = tracing_subscriber::EnvFilter::try_new(format!(
        "culino_backend={level},culino_common={level},culino_user={level},culino_recipe={level},culino_ingredient={level},culino_social={level},culino_tool={level},culino_upload={level},tower_http=debug",
        level = config.log_level
    ))
    .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new("debug"));

    let stdout_layer = tracing_subscriber::fmt::layer().with_target(true);

    let file_appender = tracing_appender::rolling::daily(&config.log_dir, "culino-backend.log");
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
