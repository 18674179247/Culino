//! 应用配置模块
//!
//! 从环境变量读取数据库连接、JWT 密钥、服务监听地址等配置。

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
    /// CORS 允许的 Origin 列表，为空则允许所有
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
}

impl AppConfig {
    /// 从环境变量加载配置，DATABASE_URL 为必填项
    pub fn from_env() -> Self {
        let run_mode = match env::var("RUN_MODE")
            .unwrap_or_else(|_| "dev".into())
            .as_str()
        {
            "production" => RunMode::Production,
            _ => RunMode::Dev,
        };

        let jwt_secret = env::var("JWT_SECRET").unwrap_or_else(|_| "dev-secret-change-me".into());

        if run_mode == RunMode::Production && jwt_secret == "dev-secret-change-me" {
            panic!("生产环境必须设置 JWT_SECRET 环境变量");
        }

        let cors_origins = env::var("CORS_ORIGINS")
            .unwrap_or_default()
            .split(',')
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty())
            .collect();

        Self {
            run_mode,
            database_url: env::var("DATABASE_URL").expect("DATABASE_URL 环境变量未设置"),
            jwt_secret,
            server_addr: env::var("SERVER_ADDR").unwrap_or_else(|_| "0.0.0.0:3000".into()),
            cors_origins,
            redis_url: env::var("REDIS_URL").unwrap_or_else(|_| "redis://127.0.0.1:6379".into()),
            log_level: env::var("LOG_LEVEL").unwrap_or_else(|_| "debug".into()),
            log_dir: env::var("LOG_DIR").unwrap_or_else(|_| "logs".into()),
            s3_endpoint: env::var("S3_ENDPOINT").unwrap_or_else(|_| "http://127.0.0.1:9000".into()),
            s3_region: env::var("S3_REGION").unwrap_or_else(|_| "us-east-1".into()),
            s3_bucket: env::var("S3_BUCKET").unwrap_or_else(|_| "menu".into()),
            s3_access_key: env::var("S3_ACCESS_KEY").unwrap_or_else(|_| "minioadmin".into()),
            s3_secret_key: env::var("S3_SECRET_KEY").unwrap_or_else(|_| "minioadmin".into()),
            deepseek_api_key: env::var("DEEPSEEK_API_KEY").ok(),
        }
    }

    pub fn is_production(&self) -> bool {
        self.run_mode == RunMode::Production
    }
}
