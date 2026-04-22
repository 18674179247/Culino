//! 标签数据访问层
//!
//! 定义 TagRepo trait 和 PostgreSQL 实现，
//! 管理标签的数据库操作，支持按类型筛选。

use async_trait::async_trait;
use sqlx::PgPool;
use menu_common::error::AppError;
use crate::model::*;

/// 标签仓储接口
#[async_trait]
pub trait TagRepo: Send + Sync {
    /// 查询标签列表，可选按类型筛选
    async fn list(&self, tag_type: Option<&str>) -> Result<Vec<Tag>, AppError>;
    /// 创建标签
    async fn create(&self, req: &CreateTagReq) -> Result<Tag, AppError>;
    /// 更新标签
    async fn update(&self, id: i32, req: &UpdateTagReq) -> Result<Tag, AppError>;
    /// 删除标签
    async fn delete(&self, id: i32) -> Result<(), AppError>;
}

/// PostgreSQL 标签仓储实现
pub struct PgTagRepo {
    pool: PgPool,
}

impl PgTagRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl TagRepo for PgTagRepo {
    async fn list(&self, tag_type: Option<&str>) -> Result<Vec<Tag>, AppError> {
        let rows = if let Some(t) = tag_type {
            sqlx::query_as::<_, Tag>("SELECT * FROM tags WHERE type = $1 ORDER BY sort_order")
                .bind(t)
                .fetch_all(&self.pool)
                .await?
        } else {
            sqlx::query_as::<_, Tag>("SELECT * FROM tags ORDER BY type, sort_order")
                .fetch_all(&self.pool)
                .await?
        };
        Ok(rows)
    }

    /// 创建标签，名称+类型唯一约束冲突时返回 Conflict 错误
    async fn create(&self, req: &CreateTagReq) -> Result<Tag, AppError> {
        let row = sqlx::query_as::<_, Tag>(
            "INSERT INTO tags (name, type, color, sort_order) VALUES ($1, $2, $3, $4) RETURNING *",
        )
        .bind(&req.name)
        .bind(&req.tag_type)
        .bind(&req.color)
        .bind(req.sort_order)
        .fetch_one(&self.pool)
        .await
        .map_err(|e| match e {
            sqlx::Error::Database(ref db_err) if db_err.is_unique_violation() => {
                AppError::Conflict("tag already exists".into())
            }
            _ => AppError::from(e),
        })?;
        Ok(row)
    }

    /// 更新标签，使用 COALESCE 实现部分更新
    async fn update(&self, id: i32, req: &UpdateTagReq) -> Result<Tag, AppError> {
        let row = sqlx::query_as::<_, Tag>(
            "UPDATE tags SET name = COALESCE($2, name), type = COALESCE($3, type), color = COALESCE($4, color), sort_order = COALESCE($5, sort_order) WHERE id = $1 RETURNING *",
        )
        .bind(id)
        .bind(&req.name)
        .bind(&req.tag_type)
        .bind(&req.color)
        .bind(req.sort_order)
        .fetch_one(&self.pool)
        .await?;
        Ok(row)
    }

    async fn delete(&self, id: i32) -> Result<(), AppError> {
        let result = sqlx::query("DELETE FROM tags WHERE id = $1")
            .bind(id)
            .execute(&self.pool)
            .await?;
        if result.rows_affected() == 0 {
            return Err(AppError::NotFound("tag not found".into()));
        }
        Ok(())
    }
}
