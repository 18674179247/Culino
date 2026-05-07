//! 食材数据访问层
//!
//! 定义 IngredientRepo trait 和 PostgreSQL 实现，
//! 管理食材和食材分类的数据库操作。

use crate::model::*;
use async_trait::async_trait;
use culino_common::error::AppError;
use sqlx::PgPool;

/// 食材仓储接口
#[async_trait]
pub trait IngredientRepo: Send + Sync {
    /// 查询所有食材
    async fn list(&self) -> Result<Vec<Ingredient>, AppError>;
    /// 根据 ID 查找食材
    async fn find_by_id(&self, id: i32) -> Result<Option<Ingredient>, AppError>;
    /// 创建食材
    async fn create(&self, req: &CreateIngredientReq) -> Result<Ingredient, AppError>;
    /// 更新食材
    async fn update(&self, id: i32, req: &UpdateIngredientReq) -> Result<Ingredient, AppError>;
    /// 删除食材
    async fn delete(&self, id: i32) -> Result<(), AppError>;
    /// 查询所有食材分类
    async fn list_categories(&self) -> Result<Vec<IngredientCategory>, AppError>;
}

/// PostgreSQL 食材仓储实现
pub struct PgIngredientRepo {
    pool: PgPool,
}

impl PgIngredientRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl IngredientRepo for PgIngredientRepo {
    async fn list(&self) -> Result<Vec<Ingredient>, AppError> {
        let rows = sqlx::query_as::<_, Ingredient>("SELECT * FROM ingredients ORDER BY id")
            .fetch_all(&self.pool)
            .await?;
        Ok(rows)
    }

    async fn find_by_id(&self, id: i32) -> Result<Option<Ingredient>, AppError> {
        let row = sqlx::query_as::<_, Ingredient>("SELECT * FROM ingredients WHERE id = $1")
            .bind(id)
            .fetch_optional(&self.pool)
            .await?;
        Ok(row)
    }

    /// 创建食材，名称唯一约束冲突时返回 Conflict 错误
    async fn create(&self, req: &CreateIngredientReq) -> Result<Ingredient, AppError> {
        let row = sqlx::query_as::<_, Ingredient>(
            "INSERT INTO ingredients (name, category_id, unit, image) VALUES ($1, $2, $3, $4) RETURNING *",
        )
            .bind(&req.name)
            .bind(req.category_id)
            .bind(&req.unit)
            .bind(&req.image)
            .fetch_one(&self.pool)
            .await
            .map_err(|e| match e {
                sqlx::Error::Database(ref db_err) if db_err.is_unique_violation() => {
                    AppError::Conflict("ingredient already exists".into())
                }
                _ => AppError::from(e),
            })?;
        Ok(row)
    }

    /// 更新食材，使用 COALESCE 实现部分更新
    async fn update(&self, id: i32, req: &UpdateIngredientReq) -> Result<Ingredient, AppError> {
        let row = sqlx::query_as::<_, Ingredient>(
            "UPDATE ingredients SET name = COALESCE($2, name), category_id = COALESCE($3, category_id), unit = COALESCE($4, unit), image = COALESCE($5, image) WHERE id = $1 RETURNING *",
        )
            .bind(id)
            .bind(&req.name)
            .bind(req.category_id)
            .bind(&req.unit)
            .bind(&req.image)
            .fetch_one(&self.pool)
            .await?;
        Ok(row)
    }

    async fn delete(&self, id: i32) -> Result<(), AppError> {
        let result = sqlx::query("DELETE FROM ingredients WHERE id = $1")
            .bind(id)
            .execute(&self.pool)
            .await?;
        if result.rows_affected() == 0 {
            return Err(AppError::NotFound("ingredient not found".into()));
        }
        Ok(())
    }

    async fn list_categories(&self) -> Result<Vec<IngredientCategory>, AppError> {
        let rows = sqlx::query_as::<_, IngredientCategory>(
            "SELECT * FROM ingredient_categories ORDER BY sort_order",
        )
        .fetch_all(&self.pool)
        .await?;
        Ok(rows)
    }
}
