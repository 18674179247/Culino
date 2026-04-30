//! CLI 命令定义

use clap::{Parser, Subcommand};

/// Culino Backend 菜谱管理后端服务
#[derive(Parser)]
#[command(name = "culino-backend", version, about)]
pub struct Cli {
    #[command(subcommand)]
    pub command: Option<Command>,
}

#[derive(Subcommand)]
pub enum Command {
    /// 启动 HTTP 服务
    Serve,
    /// 执行数据库迁移
    Migrate,
    /// 检查配置和数据库连接
    Check,
}
