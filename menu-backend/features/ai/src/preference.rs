use anyhow::{Context, Result};
use rust_decimal::Decimal;
use sqlx::PgPool;
use uuid::Uuid;

use crate::model::UserPreference;
use crate::repo::AiRepo;

pub struct PreferenceService {
    repo: AiRepo,
}

impl PreferenceService {
    pub fn new(pool: PgPool) -> Self {
        Self {
            repo: AiRepo::new(pool),
        }
    }

    /// 分析用户偏好
    pub async fn analyze_user_preference(&self, user_id: Uuid) -> Result<UserPreference> {
        tracing::info!("Analyzing preference for user {}", user_id);

        // 1. 统计收藏的菜谱标签分布
        let favorite_tags = self.analyze_favorite_tags(user_id).await?;

        // 2. 统计烹饪记录的评分和偏好
        let cooking_stats = self.analyze_cooking_logs(user_id).await?;

        // 3. 统计食材偏好
        let favorite_ingredients = self.analyze_favorite_ingredients(user_id).await?;

        // 4. 计算平均烹饪时间和难度偏好
        let (avg_cooking_time, difficulty_preference) =
            self.calculate_time_and_difficulty(user_id).await?;

        // 5. 构建用户偏好对象
        let preference = UserPreference {
            user_id,
            favorite_cuisines: Some(favorite_tags.cuisines),
            favorite_tastes: Some(favorite_tags.tastes),
            favorite_ingredients: Some(favorite_ingredients),
            favorite_tags: Some(favorite_tags.all_tags),
            dietary_restrictions: None, // 需要用户手动设置
            health_goals: None,         // 需要用户手动设置
            avg_cooking_time,
            difficulty_preference,
            total_favorites: Some(cooking_stats.total_favorites),
            total_cooking_logs: Some(cooking_stats.total_cooking_logs),
            avg_rating: cooking_stats.avg_rating,
            last_analyzed_at: None,
            updated_at: None,
        };

        // 6. 保存到数据库
        self.repo.upsert_user_preference(&preference).await?;

        tracing::info!("Preference analysis completed for user {}", user_id);

        Ok(preference)
    }

    /// 分析收藏菜谱的标签分布
    async fn analyze_favorite_tags(&self, user_id: Uuid) -> Result<FavoriteTagsAnalysis> {
        let tags = sqlx::query_as::<_, TagCount>(
            r#"
            SELECT t.name, t.type, COUNT(*) as count
            FROM favorites f
            JOIN recipe_tags rt ON f.recipe_id = rt.recipe_id
            JOIN tags t ON rt.tag_id = t.id
            WHERE f.user_id = $1
            GROUP BY t.name, t.type
            ORDER BY count DESC
            "#
        )
        .bind(user_id)
        .fetch_all(self.repo.pool())
        .await
        .context("Failed to analyze favorite tags")?;

        let total = tags.iter().map(|t| t.count).sum::<i64>() as f64;

        let mut cuisines = serde_json::Map::new();
        let mut tastes = serde_json::Map::new();
        let mut all_tags = serde_json::Map::new();

        for tag in tags {
            let weight = if total > 0.0 {
                tag.count as f64 / total
            } else {
                0.0
            };
            let weight_value = serde_json::json!(weight);

            all_tags.insert(tag.name.clone(), weight_value.clone());

            match tag.tag_type.as_str() {
                "cuisine" => {
                    cuisines.insert(tag.name, weight_value);
                }
                "taste" => {
                    tastes.insert(tag.name, weight_value);
                }
                _ => {}
            }
        }

        Ok(FavoriteTagsAnalysis {
            cuisines: serde_json::Value::Object(cuisines),
            tastes: serde_json::Value::Object(tastes),
            all_tags: serde_json::Value::Object(all_tags),
        })
    }

    /// 分析烹饪记录统计
    async fn analyze_cooking_logs(&self, user_id: Uuid) -> Result<CookingStatsAnalysis> {
        let stats = sqlx::query_as::<_, CookingStats>(
            r#"
            SELECT
                (SELECT COUNT(*) FROM favorites WHERE user_id = $1) as total_favorites,
                COUNT(*) as total_cooking_logs,
                AVG(rating) as avg_rating
            FROM cooking_logs
            WHERE user_id = $1
            "#
        )
        .bind(user_id)
        .fetch_one(self.repo.pool())
        .await
        .context("Failed to analyze cooking logs")?;

        Ok(CookingStatsAnalysis {
            total_favorites: stats.total_favorites.unwrap_or(0) as i32,
            total_cooking_logs: stats.total_cooking_logs.unwrap_or(0) as i32,
            avg_rating: stats.avg_rating,
        })
    }

    /// 分析喜欢的食材
    async fn analyze_favorite_ingredients(&self, user_id: Uuid) -> Result<serde_json::Value> {
        let ingredients = sqlx::query_as::<_, IngredientCount>(
            r#"
            SELECT ri.ingredient_id, i.name, COUNT(*) as count
            FROM favorites f
            JOIN recipe_ingredients ri ON f.recipe_id = ri.recipe_id
            JOIN ingredients i ON ri.ingredient_id = i.id
            WHERE f.user_id = $1
            GROUP BY ri.ingredient_id, i.name
            ORDER BY count DESC
            LIMIT 20
            "#
        )
        .bind(user_id)
        .fetch_all(self.repo.pool())
        .await
        .context("Failed to analyze favorite ingredients")?;

        let total = ingredients.iter().map(|i| i.count).sum::<i64>() as f64;

        let mut result = serde_json::Map::new();
        for ingredient in ingredients {
            let weight = if total > 0.0 {
                ingredient.count as f64 / total
            } else {
                0.0
            };
            result.insert(ingredient.ingredient_id.to_string(), serde_json::json!(weight));
        }

        Ok(serde_json::Value::Object(result))
    }

    /// 计算平均烹饪时间和难度偏好
    async fn calculate_time_and_difficulty(
        &self,
        user_id: Uuid,
    ) -> Result<(Option<i32>, Option<i16>)> {
        let stats = sqlx::query_as::<_, TimeAndDifficulty>(
            r#"
            SELECT
                AVG(r.cooking_time)::INT as avg_cooking_time,
                ROUND(AVG(r.difficulty))::SMALLINT as avg_difficulty
            FROM favorites f
            JOIN recipes r ON f.recipe_id = r.id
            WHERE f.user_id = $1 AND r.cooking_time IS NOT NULL
            "#
        )
        .bind(user_id)
        .fetch_one(self.repo.pool())
        .await
        .context("Failed to calculate time and difficulty")?;

        Ok((stats.avg_cooking_time, stats.avg_difficulty))
    }

    /// 获取用户偏好
    pub async fn get_user_preference(&self, user_id: Uuid) -> Result<Option<UserPreference>> {
        self.repo.get_user_preference(user_id).await
    }
}

// 辅助结构体
#[derive(sqlx::FromRow)]
struct TagCount {
    name: String,
    #[sqlx(rename = "type")]
    tag_type: String,
    count: i64,
}

#[derive(sqlx::FromRow)]
struct CookingStats {
    total_favorites: Option<i64>,
    total_cooking_logs: Option<i64>,
    avg_rating: Option<Decimal>,
}

#[derive(sqlx::FromRow)]
#[allow(dead_code)]
struct IngredientCount {
    ingredient_id: i32,
    name: String,
    count: i64,
}

#[derive(sqlx::FromRow)]
struct TimeAndDifficulty {
    avg_cooking_time: Option<i32>,
    avg_difficulty: Option<i16>,
}

struct FavoriteTagsAnalysis {
    cuisines: serde_json::Value,
    tastes: serde_json::Value,
    all_tags: serde_json::Value,
}

struct CookingStatsAnalysis {
    total_favorites: i32,
    total_cooking_logs: i32,
    avg_rating: Option<Decimal>,
}
