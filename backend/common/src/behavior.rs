//! 用户行为日志抽象
//!
//! 定义 BehaviorLogger trait，由 AI 模块实现，通过 AppState 注入。
//! 避免业务模块直接依赖 AI 模块。

use async_trait::async_trait;
use uuid::Uuid;

use crate::error::AppError;
use crate::state::AppState;

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

/// fire-and-forget 记录用户行为到 BehaviorLogger。
///
/// 若 AppState 未配置 logger(未配置 DeepSeek 时),静默跳过。
/// 失败只记 tracing,不影响业务路径;行为日志不是业务关键路径。
pub fn spawn_behavior_log(
    state: &AppState,
    user_id: Uuid,
    recipe_id: Uuid,
    action: &'static str,
    value: Option<serde_json::Value>,
) {
    let Some(logger) = state.behavior_logger.clone() else {
        return;
    };
    tokio::spawn(async move {
        if let Err(e) = logger.log(user_id, recipe_id, action, value).await {
            tracing::error!(
                "行为日志记录失败: user={user_id}, recipe={recipe_id}, action={action}, error={e}"
            );
        }
    });
}
