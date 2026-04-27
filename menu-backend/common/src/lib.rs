//! 公共模块
//!
//! 提供认证、配置、数据库、错误处理、分页、Redis、事务、状态等基础设施，
//! 被所有业务 crate 共享使用。

pub mod auth;
pub mod behavior;
pub mod config;
pub mod error;
pub mod pagination;
pub mod redis;
pub mod response;
pub mod s3;
pub mod state;
pub mod tx;
