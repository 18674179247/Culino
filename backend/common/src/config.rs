//! 应用配置模块
//!
//! 从环境变量读取数据库连接、JWT 密钥、服务监听地址等配置。
//! 关键密钥（JWT_SECRET、S3_ACCESS_KEY、S3_SECRET_KEY、DATABASE_URL）缺失时直接 panic，
//! 避免使用可预测的默认值导致安全事故。

use std::env;

/// 运行环境
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum RunMode {
    Dev,
    Production,
}

/// 应用全局配置
#[derive(Debug, Clone)]
pub struct AppConfig {
    /// 运行环境
    pub run_mode: RunMode,
    /// 数据库连接字符串
    pub database_url: String,
    /// JWT 签名密钥
    pub jwt_secret: String,
    /// HTTP 服务监听地址
    pub server_addr: String,
    /// CORS 允许的 Origin 列表，生产环境必须显式配置
    pub cors_origins: Vec<String>,
    /// Redis 连接地址
    pub redis_url: String,
    /// 日志级别
    pub log_level: String,
    /// 日志文件目录
    pub log_dir: String,
    /// S3 端点地址
    pub s3_endpoint: String,
    /// S3 区域
    pub s3_region: String,
    /// S3 存储桶名称
    pub s3_bucket: String,
    /// S3 访问密钥
    pub s3_access_key: String,
    /// S3 密钥
    pub s3_secret_key: String,
    /// DeepSeek API Key
    pub deepseek_api_key: Option<String>,
    /// 启动时自动提权为 admin 的用户名（仅当该用户已存在时生效）
    pub bootstrap_admin_username: Option<String>,
}

/// 必填环境变量，缺失直接 panic
fn require_env(key: &str) -> String {
    env::var(key).unwrap_or_else(|_| panic!("环境变量 {key} 未设置"))
}

impl AppConfig {
    /// 从环境变量加载配置，关键密钥缺失直接 panic
    pub fn from_env() -> Self {
        let run_mode = match env::var("RUN_MODE")
            .unwrap_or_else(|_| "dev".into())
            .as_str()
        {
            "production" => RunMode::Production,
            _ => RunMode::Dev,
        };

        let jwt_secret = require_env("JWT_SECRET");
        if jwt_secret.len() < 32 {
            panic!("JWT_SECRET 长度不足 32 字节，请使用 `openssl rand -base64 48` 生成强随机密钥");
        }

        let cors_origins: Vec<String> = env::var("CORS_ORIGINS")
            .unwrap_or_default()
            .split(',')
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty())
            .collect();

        if run_mode == RunMode::Production && cors_origins.is_empty() {
            panic!("生产环境必须显式配置 CORS_ORIGINS（逗号分隔的完整 Origin 列表）");
        }

        Self {
            run_mode,
            database_url: require_env("DATABASE_URL"),
            jwt_secret,
            server_addr: env::var("SERVER_ADDR").unwrap_or_else(|_| "0.0.0.0:3000".into()),
            cors_origins,
            redis_url: require_env("REDIS_URL"),
            log_level: env::var("LOG_LEVEL").unwrap_or_else(|_| "info".into()),
            log_dir: env::var("LOG_DIR").unwrap_or_else(|_| "logs".into()),
            s3_endpoint: require_env("S3_ENDPOINT"),
            s3_region: env::var("S3_REGION").unwrap_or_else(|_| "us-east-1".into()),
            s3_bucket: env::var("S3_BUCKET").unwrap_or_else(|_| "culino".into()),
            s3_access_key: require_env("S3_ACCESS_KEY"),
            s3_secret_key: require_env("S3_SECRET_KEY"),
            deepseek_api_key: env::var("DEEPSEEK_API_KEY").ok().filter(|s| !s.is_empty()),
            bootstrap_admin_username: env::var("BOOTSTRAP_ADMIN_USERNAME")
                .ok()
                .filter(|s| !s.is_empty()),
        }
    }

    pub fn is_production(&self) -> bool {
        self.run_mode == RunMode::Production
    }
}
