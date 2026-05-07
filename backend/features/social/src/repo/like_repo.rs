use anyhow::{Context, Result};
use sqlx::PgPool;
use uuid::Uuid;

pub struct LikeRepo;

impl LikeRepo {
    pub async fn toggle(pool: &PgPool, user_id: Uuid, recipe_id: Uuid) -> Result<bool> {
        // Returns true if liked, false if unliked
        let exists = sqlx::query_scalar::<_, bool>(
            "SELECT EXISTS(SELECT 1 FROM recipe_likes WHERE user_id = $1 AND recipe_id = $2)",
        )
            .bind(user_id)
            .bind(recipe_id)
            .fetch_one(pool)
            .await
            .context("Failed to check like")?;

        if exists {
            sqlx::query("DELETE FROM recipe_likes WHERE user_id = $1 AND recipe_id = $2")
                .bind(user_id)
                .bind(recipe_id)
                .execute(pool)
                .await
                .context("Failed to remove like")?;
            Ok(false)
        } else {
            sqlx::query("INSERT INTO recipe_likes (user_id, recipe_id) VALUES ($1, $2) ON CONFLICT DO NOTHING")
                .bind(user_id)
                .bind(recipe_id)
                .execute(pool)
                .await
                .context("Failed to add like")?;
            Ok(true)
        }
    }

    pub async fn count(pool: &PgPool, recipe_id: Uuid) -> Result<i64> {
        let count =
            sqlx::query_scalar::<_, i64>("SELECT COUNT(*) FROM recipe_likes WHERE recipe_id = $1")
                .bind(recipe_id)
                .fetch_one(pool)
                .await
                .context("Failed to count likes")?;
        Ok(count)
    }

    pub async fn is_liked(pool: &PgPool, user_id: Uuid, recipe_id: Uuid) -> Result<bool> {
        let exists = sqlx::query_scalar::<_, bool>(
            "SELECT EXISTS(SELECT 1 FROM recipe_likes WHERE user_id = $1 AND recipe_id = $2)",
        )
            .bind(user_id)
            .bind(recipe_id)
            .fetch_one(pool)
            .await
            .context("Failed to check like")?;
        Ok(exists)
    }
}
