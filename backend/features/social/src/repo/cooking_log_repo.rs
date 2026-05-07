//! 烹饪记录数据访问层
//!
//! 定义 CookingLogRepo trait 和 PostgreSQL 实现，
//! 管理用户的烹饪日志。所有操作都会校验用户归属权。

use crate::model::*;
use async_trait::async_trait;
use culino_common::error::AppError;
use sqlx::PgPool;
use uuid::Uuid;

/// 烹饪记录仓储接口
#[async_trait]
pub trait CookingLogRepo: Send + Sync {
    /// 查询用户的烹饪记录
    async fn list_by_user(&self, user_id: Uuid) -> Result<Vec<CookingLog>, AppError>;
    /// 创建烹饪记录
    async fn create(
        &self,
        user_id: Uuid,
        req: &CreateCookingLogReq,
    ) -> Result<CookingLog, AppError>;
    /// 更新烹饪记录
    async fn update(
        &self,
        id: Uuid,
        user_id: Uuid,
        req: &UpdateCookingLogReq,
    ) -> Result<CookingLog, AppError>;
    /// 删除烹饪记录
    async fn delete(&self, id: Uuid, user_id: Uuid) -> Result<(), AppError>;
}

/// PostgreSQL 烹饪记录仓储实现
pub struct PgCookingLogRepo {
    pool: PgPool,
}

impl PgCookingLogRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl CookingLogRepo for PgCookingLogRepo {
    /// 查询用户的烹饪记录，按烹饪日期和创建时间倒序
    async fn list_by_user(&self, user_id: Uuid) -> Result<Vec<CookingLog>, AppError> {
        let rows = sqlx::query_as::<_, CookingLog>(
            "SELECT * FROM cooking_logs WHERE user_id = $1 ORDER BY cooked_at DESC, created_at DESC",
        )
            .bind(user_id)
            .fetch_all(&self.pool)
            .await?;
        Ok(rows)
    }

    /// 创建烹饪记录，若未指定烹饪日期则默认为当天
    async fn create(
        &self,
        user_id: Uuid,
        req: &CreateCookingLogReq,
    ) -> Result<CookingLog, AppError> {
        let row = sqlx::query_as::<_, CookingLog>(
            "INSERT INTO cooking_logs (recipe_id, user_id, rating, note, cooked_at) VALUES ($1,$2,$3,$4,COALESCE($5, CURRENT_DATE)) RETURNING *",
        )
            .bind(req.recipe_id)
            .bind(user_id)
            .bind(req.rating)
            .bind(&req.note)
            .bind(req.cooked_at)
            .fetch_one(&self.pool)
            .await?;
        Ok(row)
    }

    /// 更新烹饪记录，仅允许修改自己的记录
    async fn update(
        &self,
        id: Uuid,
        user_id: Uuid,
        req: &UpdateCookingLogReq,
    ) -> Result<CookingLog, AppError> {
        let row = sqlx::query_as::<_, CookingLog>(
            "UPDATE cooking_logs SET rating = COALESCE($3, rating), note = COALESCE($4, note), updated_at = now() WHERE id = $1 AND user_id = $2 RETURNING *",
        )
            .bind(id)
            .bind(user_id)
            .bind(req.rating)
            .bind(&req.note)
            .fetch_optional(&self.pool)
            .await?
            .ok_or_else(|| AppError::NotFound("cooking log not found".into()))?;
        Ok(row)
    }

    /// 删除烹饪记录，仅允许删除自己的记录
    async fn delete(&self, id: Uuid, user_id: Uuid) -> Result<(), AppError> {
        let result = sqlx::query("DELETE FROM cooking_logs WHERE id = $1 AND user_id = $2")
            .bind(id)
            .bind(user_id)
            .execute(&self.pool)
            .await?;
        if result.rows_affected() == 0 {
            return Err(AppError::NotFound("cooking log not found".into()));
        }
        Ok(())
    }
}
