mod banner;
mod check;
mod cli;
mod openapi;
mod router;
mod server;
mod tracing;

use clap::Parser;
use cli::{Cli, Command};
use culino_common::config::AppConfig;

#[tokio::main]
async fn main() {
    dotenvy::dotenv().ok();

    let cli = Cli::parse();
    let config = AppConfig::from_env();
    let _log_guard = tracing::init_tracing(&config);

    match cli.command.unwrap_or(Command::Serve) {
        Command::Check => {
            let (pool, _redis, _s3) = check::check(&config).await;
            pool.close().await;
            ::tracing::info!("所有检查通过");
        }
        Command::Migrate => {
            let (pool, _redis, _s3) = check::check(&config).await;
            check::migrate(&pool).await;
            pool.close().await;
        }
        Command::Serve => {
            let (pool, redis, s3) = check::check(&config).await;
            check::migrate(&pool).await;
            server::serve(config, pool, redis, s3).await;
        }
    }
}
