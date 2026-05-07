//! 膳食计划数据访问层
//!
//! 定义 MealPlanRepo trait 和 PostgreSQL 实现，
//! 管理用户的膳食计划，支持按日期范围查询。
//! 同一用户同一天同一餐次有唯一约束。

use crate::model::*;
use async_trait::async_trait;
use chrono::NaiveDate;
use culino_common::error::AppError;
use sqlx::PgPool;
use uuid::Uuid;

/// 膳食计划仓储接口
#[async_trait]
pub trait MealPlanRepo: Send + Sync {
    /// 按日期范围查询膳食计划
    async fn list_by_user(
        &self,
        user_id: Uuid,
        start: Option<NaiveDate>,
        end: Option<NaiveDate>,
    ) -> Result<Vec<MealPlan>, AppError>;
    /// 创建膳食计划
    async fn create(&self, user_id: Uuid, req: &CreateMealPlanReq) -> Result<MealPlan, AppError>;
    /// 更新膳食计划
    async fn update(
        &self,
        id: Uuid,
        user_id: Uuid,
        req: &UpdateMealPlanReq,
    ) -> Result<MealPlan, AppError>;
    /// 删除膳食计划
    async fn delete(&self, id: Uuid, user_id: Uuid) -> Result<(), AppError>;
}

/// PostgreSQL 膳食计划仓储实现
pub struct PgMealPlanRepo {
    pool: PgPool,
}

impl PgMealPlanRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl MealPlanRepo for PgMealPlanRepo {
    /// 按日期范围查询膳食计划，默认从今天起未来 7 天
    async fn list_by_user(
        &self,
        user_id: Uuid,
        start: Option<NaiveDate>,
        end: Option<NaiveDate>,
    ) -> Result<Vec<MealPlan>, AppError> {
        let start = start.unwrap_or_else(|| chrono::Utc::now().date_naive());
        let end = end.unwrap_or_else(|| start + chrono::Duration::days(7));
        let rows = sqlx::query_as::<_, MealPlan>(
            "SELECT * FROM meal_plans WHERE user_id = $1 AND plan_date BETWEEN $2 AND $3 ORDER BY plan_date, meal_type",
        )
            .bind(user_id)
            .bind(start)
            .bind(end)
            .fetch_all(&self.pool)
            .await?;
        Ok(rows)
    }

    /// 创建膳食计划，唯一约束冲突时返回 Conflict 错误
    async fn create(&self, user_id: Uuid, req: &CreateMealPlanReq) -> Result<MealPlan, AppError> {
        let row = sqlx::query_as::<_, MealPlan>(
            "INSERT INTO meal_plans (user_id, recipe_id, plan_date, meal_type, note) VALUES ($1,$2,$3,$4,$5) RETURNING *",
        )
            .bind(user_id)
            .bind(req.recipe_id)
            .bind(req.plan_date)
            .bind(req.meal_type)
            .bind(&req.note)
            .fetch_one(&self.pool)
            .await
            .map_err(|e| match e {
                sqlx::Error::Database(ref db_err) if db_err.is_unique_violation() => {
                    AppError::Conflict("meal plan already exists for this date and meal type".into())
                }
                _ => AppError::from(e),
            })?;
        Ok(row)
    }

    /// 更新膳食计划，仅允许修改自己的计划
    async fn update(
        &self,
        id: Uuid,
        user_id: Uuid,
        req: &UpdateMealPlanReq,
    ) -> Result<MealPlan, AppError> {
        let row = sqlx::query_as::<_, MealPlan>(
            "UPDATE meal_plans SET recipe_id = COALESCE($3, recipe_id), note = COALESCE($4, note) WHERE id = $1 AND user_id = $2 RETURNING *",
        )
            .bind(id)
            .bind(user_id)
            .bind(req.recipe_id)
            .bind(&req.note)
            .fetch_optional(&self.pool)
            .await?
            .ok_or_else(|| AppError::NotFound("meal plan not found".into()))?;
        Ok(row)
    }

    /// 删除膳食计划，仅允许删除自己的计划
    async fn delete(&self, id: Uuid, user_id: Uuid) -> Result<(), AppError> {
        let result = sqlx::query("DELETE FROM meal_plans WHERE id = $1 AND user_id = $2")
            .bind(id)
            .bind(user_id)
            .execute(&self.pool)
            .await?;
        if result.rows_affected() == 0 {
            return Err(AppError::NotFound("meal plan not found".into()));
        }
        Ok(())
    }
}
