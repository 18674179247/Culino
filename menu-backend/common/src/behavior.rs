//! 用户行为日志抽象
//!
//! 定义 BehaviorLogger trait，由 AI 模块实现，通过 AppState 注入。
//! 避免业务模块直接依赖 AI 模块。

use async_trait::async_trait;
use uuid::Uuid;

use crate::error::AppError;

/// 用户行为日志记录器接口
#[async_trait]
pub trait BehaviorLogger: Send + Sync {
    /// 记录用户行为
    async fn log(
        &self,
        user_id: Uuid,
        recipe_id: Uuid,
        action_type: &str,
        action_value: Option<serde_json::Value>,
    ) -> Result<(), AppError>;
}
