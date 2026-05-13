//! 评论数据访问层

use crate::model::RecipeComment;
use culino_common::error::AppError;
use sqlx::PgPool;
use uuid::Uuid;

pub struct CommentRepo;

impl CommentRepo {
    pub async fn list(
        pool: &PgPool,
        recipe_id: Uuid,
        page: i64,
        page_size: i64,
    ) -> Result<(Vec<RecipeComment>, i64), AppError> {
        let offset = (page - 1) * page_size;
        let comments = sqlx::query_as::<_, RecipeComment>(
            r#"
            SELECT c.id, c.recipe_id, c.user_id, u.username, u.nickname, u.avatar, c.content, c.created_at
            FROM recipe_comments c
            JOIN users u ON c.user_id = u.id
            WHERE c.recipe_id = $1
            ORDER BY c.created_at DESC
            LIMIT $2 OFFSET $3
            "#,
        )
            .bind(recipe_id)
            .bind(page_size)
            .bind(offset)
            .fetch_all(pool)
            .await?;

        let total: i64 = sqlx::query_scalar(
            "SELECT COUNT(*) FROM recipe_comments WHERE recipe_id = $1",
        )
        .bind(recipe_id)
        .fetch_one(pool)
        .await?;

        Ok((comments, total))
    }

    pub async fn create(
        pool: &PgPool,
        user_id: Uuid,
        recipe_id: Uuid,
        content: &str,
    ) -> Result<RecipeComment, AppError> {
        let comment = sqlx::query_as::<_, RecipeComment>(
            r#"
            WITH inserted AS (
                INSERT INTO recipe_comments (user_id, recipe_id, content)
                VALUES ($1, $2, $3)
                RETURNING *
            )
            SELECT i.id, i.recipe_id, i.user_id, u.username, u.nickname, u.avatar, i.content, i.created_at
            FROM inserted i
            JOIN users u ON i.user_id = u.id
            "#,
        )
            .bind(user_id)
            .bind(recipe_id)
            .bind(content)
            .fetch_one(pool)
            .await?;
        Ok(comment)
    }

    pub async fn delete(
        pool: &PgPool,
        comment_id: Uuid,
        user_id: Uuid,
    ) -> Result<bool, AppError> {
        let result = sqlx::query("DELETE FROM recipe_comments WHERE id = $1 AND user_id = $2")
            .bind(comment_id)
            .bind(user_id)
            .execute(pool)
            .await?;
        Ok(result.rows_affected() > 0)
    }

    /// 管理员删除评论,跳过 user_id 校验,用于处理违规内容
    pub async fn delete_as_admin(pool: &PgPool, comment_id: Uuid) -> Result<bool, AppError> {
        let result = sqlx::query("DELETE FROM recipe_comments WHERE id = $1")
            .bind(comment_id)
            .execute(pool)
            .await?;
        Ok(result.rows_affected() > 0)
    }

    pub async fn count(pool: &PgPool, recipe_id: Uuid) -> Result<i64, AppError> {
        let count: i64 = sqlx::query_scalar(
            "SELECT COUNT(*) FROM recipe_comments WHERE recipe_id = $1",
        )
        .bind(recipe_id)
        .fetch_one(pool)
        .await?;
        Ok(count)
    }
}
