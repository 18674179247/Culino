//! 收藏数据访问层
//!
//! 定义 FavoriteRepo trait 和 PostgreSQL 实现，
//! 管理用户与菜谱的收藏关系，使用 ON CONFLICT 处理重复收藏。

use crate::model::{Favorite, FavoriteWithTitle};
use async_trait::async_trait;
use culino_common::error::AppError;
use sqlx::PgPool;
use uuid::Uuid;

/// 收藏仓储接口
#[async_trait]
pub trait FavoriteRepo: Send + Sync {
    /// 查询用户的所有收藏（含菜谱标题）
    async fn list_by_user(&self, user_id: Uuid) -> Result<Vec<FavoriteWithTitle>, AppError>;
    /// 添加收藏（幂等操作）
    async fn add(&self, user_id: Uuid, recipe_id: Uuid) -> Result<Favorite, AppError>;
    /// 取消收藏
    async fn remove(&self, user_id: Uuid, recipe_id: Uuid) -> Result<(), AppError>;
}

/// PostgreSQL 收藏仓储实现
pub struct PgFavoriteRepo {
    pool: PgPool,
}

impl PgFavoriteRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl FavoriteRepo for PgFavoriteRepo {
    /// 查询用户的所有收藏，按时间倒序，JOIN 菜谱表获取摘要信息
    /// 只返回上架中(status=1)的菜谱,下架/软删的收藏记录不展示
    async fn list_by_user(&self, user_id: Uuid) -> Result<Vec<FavoriteWithTitle>, AppError> {
        let rows = sqlx::query_as::<_, FavoriteWithTitle>(
            "SELECT f.user_id, f.recipe_id, f.created_at, \
             r.title as recipe_title, r.cover_image, r.difficulty, r.cooking_time, r.servings \
             FROM favorites f INNER JOIN recipes r ON f.recipe_id = r.id \
             WHERE f.user_id = $1 AND r.status = 1 ORDER BY f.created_at DESC",
        )
        .bind(user_id)
        .fetch_all(&self.pool)
        .await?;
        Ok(rows)
    }

    /// 添加收藏，若已存在则直接返回已有记录（幂等操作）
    async fn add(&self, user_id: Uuid, recipe_id: Uuid) -> Result<Favorite, AppError> {
        let row = sqlx::query_as::<_, Favorite>(
            "INSERT INTO favorites (user_id, recipe_id) VALUES ($1, $2) ON CONFLICT DO NOTHING RETURNING *",
        )
            .bind(user_id)
            .bind(recipe_id)
            .fetch_optional(&self.pool)
            .await?;
        match row {
            Some(f) => Ok(f),
            None => {
                // 已存在，直接查询返回
                let f = sqlx::query_as::<_, Favorite>(
                    "SELECT * FROM favorites WHERE user_id = $1 AND recipe_id = $2",
                )
                .bind(user_id)
                .bind(recipe_id)
                .fetch_one(&self.pool)
                .await?;
                Ok(f)
            }
        }
    }

    async fn remove(&self, user_id: Uuid, recipe_id: Uuid) -> Result<(), AppError> {
        sqlx::query("DELETE FROM favorites WHERE user_id = $1 AND recipe_id = $2")
            .bind(user_id)
            .bind(recipe_id)
            .execute(&self.pool)
            .await?;
        Ok(())
    }
}
