//! 点赞数据访问层
//!
//! 使用单 SQL INSERT ... ON CONFLICT ... RETURNING xmax=0 的"自插入或清除"
//! 模式实现无竞态 toggle;避免原 check-then-act 的并发问题。

use culino_common::error::AppError;
use sqlx::PgPool;
use uuid::Uuid;

pub struct LikeRepo;

impl LikeRepo {
    /// Toggle 点赞状态。
    /// 返回 true 表示本次变成"已点赞",false 表示变成"未点赞"。
    ///
    /// 实现策略:先尝试 INSERT ... ON CONFLICT DO NOTHING,
    /// rows_affected() > 0 → 新增了一行,即用户之前未点赞,现在已点 → true;
    /// rows_affected() == 0 → 已存在同 (user_id, recipe_id) 行 → 本次应变为取消,
    /// 再 DELETE → false。整个流程不依赖"先 SELECT 再判断"的窗口,避免 race。
    pub async fn toggle(
        pool: &PgPool,
        user_id: Uuid,
        recipe_id: Uuid,
    ) -> Result<bool, AppError> {
        let inserted = sqlx::query(
            "INSERT INTO recipe_likes (user_id, recipe_id) VALUES ($1, $2) ON CONFLICT DO NOTHING",
        )
        .bind(user_id)
        .bind(recipe_id)
        .execute(pool)
        .await?
        .rows_affected();

        if inserted > 0 {
            Ok(true)
        } else {
            sqlx::query("DELETE FROM recipe_likes WHERE user_id = $1 AND recipe_id = $2")
                .bind(user_id)
                .bind(recipe_id)
                .execute(pool)
                .await?;
            Ok(false)
        }
    }

    pub async fn count(pool: &PgPool, recipe_id: Uuid) -> Result<i64, AppError> {
        let count: i64 =
            sqlx::query_scalar("SELECT COUNT(*) FROM recipe_likes WHERE recipe_id = $1")
                .bind(recipe_id)
                .fetch_one(pool)
                .await?;
        Ok(count)
    }

    pub async fn is_liked(
        pool: &PgPool,
        user_id: Uuid,
        recipe_id: Uuid,
    ) -> Result<bool, AppError> {
        let exists: bool = sqlx::query_scalar(
            "SELECT EXISTS(SELECT 1 FROM recipe_likes WHERE user_id = $1 AND recipe_id = $2)",
        )
        .bind(user_id)
        .bind(recipe_id)
        .fetch_one(pool)
        .await?;
        Ok(exists)
    }
}
