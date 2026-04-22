//! 调料数据访问层
//!
//! 定义 SeasoningRepo trait 和 PostgreSQL 实现，
//! 管理调料的数据库操作。

use async_trait::async_trait;
use sqlx::PgPool;
use menu_common::error::AppError;
use crate::model::*;

/// 调料仓储接口
#[async_trait]
pub trait SeasoningRepo: Send + Sync {
    /// 查询所有调料
    async fn list(&self) -> Result<Vec<Seasoning>, AppError>;
    /// 创建调料
    async fn create(&self, req: &CreateSeasoningReq) -> Result<Seasoning, AppError>;
    /// 更新调料
    async fn update(&self, id: i32, req: &UpdateSeasoningReq) -> Result<Seasoning, AppError>;
    /// 删除调料
    async fn delete(&self, id: i32) -> Result<(), AppError>;
}

/// PostgreSQL 调料仓储实现
pub struct PgSeasoningRepo {
    pool: PgPool,
}

impl PgSeasoningRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl SeasoningRepo for PgSeasoningRepo {
    async fn list(&self) -> Result<Vec<Seasoning>, AppError> {
        let rows = sqlx::query_as::<_, Seasoning>("SELECT * FROM seasonings ORDER BY id")
            .fetch_all(&self.pool)
            .await?;
        Ok(rows)
    }

    /// 创建调料，名称唯一约束冲突时返回 Conflict 错误
    async fn create(&self, req: &CreateSeasoningReq) -> Result<Seasoning, AppError> {
        let row = sqlx::query_as::<_, Seasoning>(
            "INSERT INTO seasonings (name, unit, image) VALUES ($1, $2, $3) RETURNING *",
        )
        .bind(&req.name)
        .bind(&req.unit)
        .bind(&req.image)
        .fetch_one(&self.pool)
        .await
        .map_err(|e| match e {
            sqlx::Error::Database(ref db_err) if db_err.is_unique_violation() => {
                AppError::Conflict("seasoning already exists".into())
            }
            _ => AppError::from(e),
        })?;
        Ok(row)
    }

    /// 更新调料，使用 COALESCE 实现部分更新
    async fn update(&self, id: i32, req: &UpdateSeasoningReq) -> Result<Seasoning, AppError> {
        let row = sqlx::query_as::<_, Seasoning>(
            "UPDATE seasonings SET name = COALESCE($2, name), unit = COALESCE($3, unit), image = COALESCE($4, image) WHERE id = $1 RETURNING *",
        )
        .bind(id)
        .bind(&req.name)
        .bind(&req.unit)
        .bind(&req.image)
        .fetch_one(&self.pool)
        .await?;
        Ok(row)
    }

    async fn delete(&self, id: i32) -> Result<(), AppError> {
        let result = sqlx::query("DELETE FROM seasonings WHERE id = $1")
            .bind(id)
            .execute(&self.pool)
            .await?;
        if result.rows_affected() == 0 {
            return Err(AppError::NotFound("seasoning not found".into()));
        }
        Ok(())
    }
}
