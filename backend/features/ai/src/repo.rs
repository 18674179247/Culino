use crate::model::*;
use anyhow::{Context, Result};
use async_trait::async_trait;
use culino_common::behavior::BehaviorLogger;
use culino_common::error::AppError;
use sqlx::PgPool;
use uuid::Uuid;

pub struct AiRepo {
    pool: PgPool,
}

impl AiRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    pub fn into_behavior_logger(self) -> std::sync::Arc<dyn BehaviorLogger> {
        std::sync::Arc::new(self)
    }
}

#[async_trait]
impl BehaviorLogger for AiRepo {
    async fn log(
        &self,
        user_id: Uuid,
        recipe_id: Uuid,
        action_type: &str,
        action_value: Option<serde_json::Value>,
    ) -> Result<(), AppError> {
        self.log_user_behavior(user_id, recipe_id, action_type, action_value)
            .await
            .map(|_| ())
            .map_err(|e| AppError::Internal(e))
    }
}

impl AiRepo {
    /// 获取 pool 的引用（用于其他服务）
    pub(crate) fn pool(&self) -> &PgPool {
        &self.pool
    }

    // ============================================
    // 营养分析相关
    // ============================================

    /// 获取菜谱营养分析
    pub async fn get_nutrition(&self, recipe_id: Uuid) -> Result<Option<RecipeNutrition>> {
        let nutrition = sqlx::query_as::<_, RecipeNutrition>(
            "SELECT * FROM recipe_nutrition WHERE recipe_id = $1",
        )
        .bind(recipe_id)
        .fetch_optional(&self.pool)
        .await
        .context("Failed to fetch nutrition")?;

        Ok(nutrition)
    }

    /// 保存或更新营养分析
    pub async fn upsert_nutrition(&self, nutrition: &RecipeNutrition) -> Result<()> {
        sqlx::query(
            r#"
            INSERT INTO recipe_nutrition (
                recipe_id, calories, protein, fat, carbohydrate, fiber, sodium,
                analysis_text, health_score, health_tags, suitable_for, cautions,
                serving_size, traffic_light, overall_rating, summary
            ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16)
            ON CONFLICT (recipe_id) DO UPDATE SET
                calories = EXCLUDED.calories,
                protein = EXCLUDED.protein,
                fat = EXCLUDED.fat,
                carbohydrate = EXCLUDED.carbohydrate,
                fiber = EXCLUDED.fiber,
                sodium = EXCLUDED.sodium,
                analysis_text = EXCLUDED.analysis_text,
                health_score = EXCLUDED.health_score,
                health_tags = EXCLUDED.health_tags,
                suitable_for = EXCLUDED.suitable_for,
                cautions = EXCLUDED.cautions,
                serving_size = EXCLUDED.serving_size,
                traffic_light = EXCLUDED.traffic_light,
                overall_rating = EXCLUDED.overall_rating,
                summary = EXCLUDED.summary,
                updated_at = now()
            "#,
        )
        .bind(nutrition.recipe_id)
        .bind(nutrition.calories)
        .bind(nutrition.protein)
        .bind(nutrition.fat)
        .bind(nutrition.carbohydrate)
        .bind(nutrition.fiber)
        .bind(nutrition.sodium)
        .bind(&nutrition.analysis_text)
        .bind(nutrition.health_score)
        .bind(&nutrition.health_tags)
        .bind(&nutrition.suitable_for)
        .bind(&nutrition.cautions)
        .bind(&nutrition.serving_size)
        .bind(&nutrition.traffic_light)
        .bind(&nutrition.overall_rating)
        .bind(&nutrition.summary)
        .execute(&self.pool)
        .await
        .context("Failed to upsert nutrition")?;

        Ok(())
    }

    // ============================================
    // 用户偏好相关
    // ============================================

    /// 获取用户偏好
    pub async fn get_user_preference(&self, user_id: Uuid) -> Result<Option<UserPreference>> {
        let preference = sqlx::query_as::<_, UserPreference>(
            "SELECT * FROM user_preferences WHERE user_id = $1",
        )
        .bind(user_id)
        .fetch_optional(&self.pool)
        .await
        .context("Failed to fetch user preference")?;

        Ok(preference)
    }

    /// 保存或更新用户偏好
    pub async fn upsert_user_preference(&self, preference: &UserPreference) -> Result<()> {
        sqlx::query(
            r#"
            INSERT INTO user_preferences (
                user_id, favorite_cuisines, favorite_tastes, favorite_ingredients, favorite_tags,
                dietary_restrictions, health_goals, avg_cooking_time, difficulty_preference,
                total_favorites, total_cooking_logs, avg_rating, last_analyzed_at
            ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, now())
            ON CONFLICT (user_id) DO UPDATE SET
                favorite_cuisines = EXCLUDED.favorite_cuisines,
                favorite_tastes = EXCLUDED.favorite_tastes,
                favorite_ingredients = EXCLUDED.favorite_ingredients,
                favorite_tags = EXCLUDED.favorite_tags,
                dietary_restrictions = EXCLUDED.dietary_restrictions,
                health_goals = EXCLUDED.health_goals,
                avg_cooking_time = EXCLUDED.avg_cooking_time,
                difficulty_preference = EXCLUDED.difficulty_preference,
                total_favorites = EXCLUDED.total_favorites,
                total_cooking_logs = EXCLUDED.total_cooking_logs,
                avg_rating = EXCLUDED.avg_rating,
                last_analyzed_at = now()
            "#,
        )
        .bind(preference.user_id)
        .bind(&preference.favorite_cuisines)
        .bind(&preference.favorite_tastes)
        .bind(&preference.favorite_ingredients)
        .bind(&preference.favorite_tags)
        .bind(&preference.dietary_restrictions)
        .bind(&preference.health_goals)
        .bind(preference.avg_cooking_time)
        .bind(preference.difficulty_preference)
        .bind(preference.total_favorites)
        .bind(preference.total_cooking_logs)
        .bind(preference.avg_rating)
        .execute(&self.pool)
        .await
        .context("Failed to upsert user preference")?;

        Ok(())
    }

    // ============================================
    // 推荐记录相关
    // ============================================

    /// 保存推荐记录
    pub async fn create_recommendation(&self, rec: &AiRecommendation) -> Result<Uuid> {
        let id = sqlx::query_scalar::<_, Uuid>(
            r#"
            INSERT INTO ai_recommendations (
                user_id, recipe_id, recommendation_type, score, reason
            ) VALUES ($1, $2, $3, $4, $5)
            RETURNING id
            "#,
        )
        .bind(rec.user_id)
        .bind(rec.recipe_id)
        .bind(&rec.recommendation_type)
        .bind(rec.score)
        .bind(&rec.reason)
        .fetch_one(&self.pool)
        .await
        .context("Failed to create recommendation")?;

        Ok(id)
    }

    /// 标记推荐为已点击
    pub async fn mark_recommendation_clicked(&self, recommendation_id: Uuid) -> Result<()> {
        sqlx::query(
            "UPDATE ai_recommendations SET clicked = true, clicked_at = now() WHERE id = $1",
        )
        .bind(recommendation_id)
        .execute(&self.pool)
        .await
        .context("Failed to mark recommendation as clicked")?;

        Ok(())
    }

    /// 获取用户最近的推荐记录
    pub async fn get_recent_recommendations(
        &self,
        user_id: Uuid,
        limit: i64,
    ) -> Result<Vec<AiRecommendation>> {
        let recommendations = sqlx::query_as::<_, AiRecommendation>(
            r#"
            SELECT * FROM ai_recommendations
            WHERE user_id = $1
            ORDER BY created_at DESC
            LIMIT $2
            "#,
        )
        .bind(user_id)
        .bind(limit)
        .fetch_all(&self.pool)
        .await
        .context("Failed to fetch recent recommendations")?;

        Ok(recommendations)
    }

    // ============================================
    // 行为日志相关
    // ============================================

    /// 记录用户行为
    pub async fn log_user_behavior(
        &self,
        user_id: Uuid,
        recipe_id: Uuid,
        action_type: &str,
        action_value: Option<serde_json::Value>,
    ) -> Result<Uuid> {
        let id = sqlx::query_scalar::<_, Uuid>(
            r#"
            INSERT INTO user_behavior_logs (user_id, recipe_id, action_type, action_value)
            VALUES ($1, $2, $3, $4)
            RETURNING id
            "#,
        )
        .bind(user_id)
        .bind(recipe_id)
        .bind(action_type)
        .bind(action_value)
        .fetch_one(&self.pool)
        .await
        .context("Failed to log user behavior")?;

        Ok(id)
    }

    /// 获取用户行为统计（用于偏好分析）
    pub async fn get_user_behavior_stats(&self, user_id: Uuid) -> Result<serde_json::Value> {
        let stats = sqlx::query_scalar::<_, serde_json::Value>(
            r#"
            SELECT json_build_object(
                'total_views', COUNT(*) FILTER (WHERE action_type = 'view'),
                'total_favorites', COUNT(*) FILTER (WHERE action_type = 'favorite'),
                'total_cooks', COUNT(*) FILTER (WHERE action_type = 'cook'),
                'total_ratings', COUNT(*) FILTER (WHERE action_type = 'rate')
            )
            FROM user_behavior_logs
            WHERE user_id = $1
            "#,
        )
        .bind(user_id)
        .fetch_one(&self.pool)
        .await
        .context("Failed to fetch user behavior stats")?;

        Ok(stats)
    }
}
