use anyhow::{Context, Result};
use chrono::{DateTime, Utc};
use rust_decimal::Decimal;
use sqlx::PgPool;
use uuid::Uuid;

use crate::model::{AiRecommendation, RecommendationItem};
use crate::repo::AiRepo;

pub struct RecommendationService {
    repo: AiRepo,
}

impl RecommendationService {
    pub fn new(pool: PgPool) -> Self {
        Self {
            repo: AiRepo::new(pool),
        }
    }

    /// 个性化推荐（基于用户偏好）
    pub async fn personalized_recommendations(
        &self,
        user_id: Uuid,
        limit: i64,
    ) -> Result<Vec<RecommendationItem>> {
        tracing::info!("Generating personalized recommendations for user {}", user_id);

        // 1. 获取用户偏好
        let preference = self.repo.get_user_preference(user_id).await?;

        if preference.is_none() {
            tracing::warn!("No preference found for user {}, returning trending", user_id);
            return self.trending_recommendations(limit).await;
        }

        let pref = preference.unwrap();

        // 2. 基于偏好查询菜谱
        let recipes = self.query_recipes_by_preference(user_id, &pref, limit).await?;

        // 3. 计算推荐分数并生成推荐理由
        let mut recommendations = Vec::new();
        for recipe in recipes {
            let score = self.calculate_recommendation_score(&recipe, &pref);
            let reason = self.generate_recommendation_reason(&recipe, &pref);

            // 保存推荐记录
            let rec = AiRecommendation {
                id: Uuid::new_v4(),
                user_id: Some(user_id),
                recipe_id: Some(recipe.id),
                recommendation_type: "personalized".to_string(),
                score: Decimal::try_from(score).unwrap_or_default(),
                reason: Some(reason.clone()),
                clicked: Some(false),
                clicked_at: None,
                created_at: None,
            };
            let _ = self.repo.create_recommendation(&rec).await;

            recommendations.push(RecommendationItem {
                recipe_id: recipe.id,
                title: recipe.title,
                cover_image: recipe.cover_image,
                score,
                reason,
                recommendation_type: "personalized".to_string(),
            });
        }

        // 按分数排序
        recommendations.sort_by(|a, b| b.score.partial_cmp(&a.score).unwrap());

        Ok(recommendations)
    }

    /// 相似菜谱推荐
    pub async fn similar_recommendations(
        &self,
        recipe_id: Uuid,
        limit: i64,
    ) -> Result<Vec<RecommendationItem>> {
        tracing::info!("Generating similar recommendations for recipe {}", recipe_id);

        // 1. 获取目标菜谱的标签
        let tags = self.get_recipe_tags(recipe_id).await?;

        if tags.is_empty() {
            return Ok(Vec::new());
        }

        // 2. 查找具有相似标签的菜谱
        let recipes = self.query_similar_recipes(recipe_id, &tags, limit).await?;

        // 3. 生成推荐项
        let recommendations = recipes
            .into_iter()
            .map(|recipe| RecommendationItem {
                recipe_id: recipe.id,
                title: recipe.title,
                cover_image: recipe.cover_image,
                score: recipe.similarity_score.unwrap_or(0.0),
                reason: format!("与您浏览的菜谱有 {} 个相似标签", recipe.common_tags.unwrap_or(0)),
                recommendation_type: "similar".to_string(),
            })
            .collect();

        Ok(recommendations)
    }

    /// 热门推荐（基于收藏、评分和发布时间衰减）
    pub async fn trending_recommendations(&self, limit: i64) -> Result<Vec<RecommendationItem>> {
        tracing::info!("Generating trending recommendations");

        let recipes = sqlx::query_as::<_, TrendingRecipe>(
            r#"
            SELECT
                r.id,
                r.title,
                r.cover_image,
                r.created_at,
                COUNT(DISTINCT f.user_id) as favorite_count,
                COALESCE(AVG(cl.rating), 0) as avg_rating,
                (
                    COUNT(DISTINCT f.user_id) * 0.7
                    + COALESCE(AVG(cl.rating), 0) * 0.3
                    + GREATEST(0, 1.0 - EXTRACT(EPOCH FROM (now() - r.created_at)) / (30 * 86400)) * 2.0
                ) as score
            FROM recipes r
            LEFT JOIN favorites f ON r.id = f.recipe_id
            LEFT JOIN cooking_logs cl ON r.id = cl.recipe_id
            WHERE r.status = 1
            GROUP BY r.id, r.title, r.cover_image, r.created_at
            ORDER BY score DESC
            LIMIT $1
            "#
        )
        .bind(limit)
        .fetch_all(self.repo.pool())
        .await
        .context("Failed to fetch trending recipes")?;

        let recommendations = recipes
            .into_iter()
            .map(|recipe| {
                let fav_count = recipe.favorite_count.unwrap_or(0);
                let avg_r = recipe.avg_rating.unwrap_or_default();
                let reason = if fav_count > 0 || avg_r > rust_decimal::Decimal::ZERO {
                    format!("已有 {fav_count} 人收藏，平均评分 {avg_r:.1}")
                } else {
                    "新发布的菜谱，快来尝尝鲜".into()
                };
                RecommendationItem {
                    recipe_id: recipe.id,
                    title: recipe.title,
                    cover_image: recipe.cover_image,
                    score: recipe.score.unwrap_or(0.0),
                    reason,
                    recommendation_type: "trending".to_string(),
                }
            })
            .collect();

        Ok(recommendations)
    }

    /// 基于健康目标的推荐
    pub async fn health_goal_recommendations(
        &self,
        _user_id: Uuid,
        health_goal: &str,
        limit: i64,
    ) -> Result<Vec<RecommendationItem>> {
        tracing::info!("Generating health goal recommendations: {}", health_goal);

        // 根据健康目标选择合适的营养标签
        let target_tags = match health_goal {
            "减脂" => vec!["低脂", "低热量", "高纤维"],
            "增肌" => vec!["高蛋白", "低脂"],
            "保持健康" => vec!["营养均衡", "低钠"],
            _ => vec![],
        };

        if target_tags.is_empty() {
            return Ok(Vec::new());
        }

        // 查询符合健康目标的菜谱
        let recipes = sqlx::query_as::<_, HealthRecipe>(
            r#"
            SELECT
                r.id,
                r.title,
                r.cover_image,
                rn.health_score,
                rn.health_tags
            FROM recipes r
            JOIN recipe_nutrition rn ON r.id = rn.recipe_id
            WHERE r.status = 1
              AND rn.health_tags && $1
              AND rn.health_score >= 70
            ORDER BY rn.health_score DESC
            LIMIT $2
            "#
        )
        .bind(&target_tags)
        .bind(limit)
        .fetch_all(self.repo.pool())
        .await
        .context("Failed to fetch health goal recipes")?;

        let recommendations = recipes
            .into_iter()
            .map(|recipe| {
                let matching_tags = recipe
                    .health_tags
                    .unwrap_or_default()
                    .iter()
                    .filter(|tag| target_tags.contains(&tag.as_str()))
                    .cloned()
                    .collect::<Vec<_>>()
                    .join("、");

                RecommendationItem {
                    recipe_id: recipe.id,
                    title: recipe.title,
                    cover_image: recipe.cover_image,
                    score: recipe.health_score.unwrap_or(0) as f64,
                    reason: format!("符合您的健康目标：{}", matching_tags),
                    recommendation_type: "health_goal".to_string(),
                }
            })
            .collect();

        Ok(recommendations)
    }

    // ============================================
    // 辅助方法
    // ============================================

    /// 基于用户偏好查询菜谱
    async fn query_recipes_by_preference(
        &self,
        user_id: Uuid,
        preference: &crate::model::UserPreference,
        limit: i64,
    ) -> Result<Vec<RecipeForRecommendation>> {
        // 提取偏好标签
        let favorite_tags = self.extract_top_tags(&preference.favorite_tags, 5);

        let recipes = sqlx::query_as::<_, RecipeForRecommendation>(
            r#"
            SELECT DISTINCT
                r.id,
                r.title,
                r.cover_image,
                r.difficulty,
                r.cooking_time
            FROM recipes r
            JOIN recipe_tags rt ON r.id = rt.recipe_id
            JOIN tags t ON rt.tag_id = t.id
            WHERE r.status = 1
              AND t.name = ANY($1)
              AND r.id NOT IN (SELECT recipe_id FROM favorites WHERE user_id = $2)
            ORDER BY RANDOM()
            LIMIT $3
            "#
        )
        .bind(&favorite_tags)
        .bind(user_id)
        .bind(limit * 2) // 多查询一些，后续过滤
        .fetch_all(self.repo.pool())
        .await
        .context("Failed to query recipes by preference")?;

        Ok(recipes)
    }

    /// 提取 JSONB 中权重最高的标签
    fn extract_top_tags(&self, tags_json: &Option<serde_json::Value>, top_n: usize) -> Vec<String> {
        if let Some(serde_json::Value::Object(map)) = tags_json {
            let mut tags: Vec<(String, f64)> = map
                .iter()
                .filter_map(|(k, v)| {
                    v.as_f64().map(|weight| (k.clone(), weight))
                })
                .collect();

            tags.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap());
            tags.into_iter().take(top_n).map(|(k, _)| k).collect()
        } else {
            Vec::new()
        }
    }

    /// 计算推荐分数
    fn calculate_recommendation_score(
        &self,
        recipe: &RecipeForRecommendation,
        preference: &crate::model::UserPreference,
    ) -> f64 {
        let mut score = 50.0; // 基础分

        // 难度匹配
        if let (Some(recipe_diff), Some(pref_diff)) = (recipe.difficulty, preference.difficulty_preference) {
            let diff_delta = (recipe_diff - pref_diff).abs();
            score += (5 - diff_delta).max(0) as f64 * 5.0;
        }

        // 烹饪时间匹配
        if let (Some(recipe_time), Some(pref_time)) = (recipe.cooking_time, preference.avg_cooking_time) {
            let time_delta = (recipe_time - pref_time).abs();
            if time_delta <= 15 {
                score += 10.0;
            }
        }

        score.min(100.0)
    }

    /// 基于偏好数据生成有实际意义的推荐理由
    fn generate_recommendation_reason(
        &self,
        recipe: &RecipeForRecommendation,
        preference: &crate::model::UserPreference,
    ) -> String {
        let mut reasons: Vec<String> = Vec::new();

        // 难度匹配
        if let (Some(recipe_diff), Some(pref_diff)) =
            (recipe.difficulty, preference.difficulty_preference)
        {
            if (recipe_diff - pref_diff).abs() <= 1 {
                reasons.push("烹饪难度适合您".into());
            }
        }

        // 烹饪时间匹配
        if let (Some(recipe_time), Some(pref_time)) =
            (recipe.cooking_time, preference.avg_cooking_time)
        {
            if (recipe_time - pref_time).abs() <= 15 {
                reasons.push("制作时间符合您的习惯".into());
            }
        }

        // 兜底
        if reasons.is_empty() {
            reasons.push("为您精心挑选".into());
        }

        reasons.join("，")
    }

    /// 获取菜谱标签
    async fn get_recipe_tags(&self, recipe_id: Uuid) -> Result<Vec<String>> {
        let tags = sqlx::query_scalar::<_, String>(
            r#"
            SELECT t.name
            FROM recipe_tags rt
            JOIN tags t ON rt.tag_id = t.id
            WHERE rt.recipe_id = $1
            "#
        )
        .bind(recipe_id)
        .fetch_all(self.repo.pool())
        .await
        .context("Failed to fetch recipe tags")?;

        Ok(tags)
    }

    /// 查询相似菜谱
    async fn query_similar_recipes(
        &self,
        recipe_id: Uuid,
        tags: &[String],
        limit: i64,
    ) -> Result<Vec<SimilarRecipe>> {
        let recipes = sqlx::query_as::<_, SimilarRecipe>(
            r#"
            SELECT
                r.id,
                r.title,
                r.cover_image,
                COUNT(*) as common_tags,
                (COUNT(*) * 100.0 / $3) as similarity_score
            FROM recipes r
            JOIN recipe_tags rt ON r.id = rt.recipe_id
            JOIN tags t ON rt.tag_id = t.id
            WHERE r.id != $1
              AND r.status = 1
              AND t.name = ANY($2)
            GROUP BY r.id, r.title, r.cover_image
            ORDER BY common_tags DESC, r.created_at DESC
            LIMIT $4
            "#
        )
        .bind(recipe_id)
        .bind(tags)
        .bind(tags.len() as i64)
        .bind(limit)
        .fetch_all(self.repo.pool())
        .await
        .context("Failed to query similar recipes")?;

        Ok(recipes)
    }
}

// 辅助结构体
#[derive(sqlx::FromRow)]
struct RecipeForRecommendation {
    id: Uuid,
    title: String,
    cover_image: Option<String>,
    difficulty: Option<i16>,
    cooking_time: Option<i32>,
}

#[derive(sqlx::FromRow)]
#[allow(dead_code)]
struct TrendingRecipe {
    id: Uuid,
    title: String,
    cover_image: Option<String>,
    created_at: Option<DateTime<Utc>>,
    favorite_count: Option<i64>,
    avg_rating: Option<Decimal>,
    score: Option<f64>,
}

#[derive(sqlx::FromRow)]
struct HealthRecipe {
    id: Uuid,
    title: String,
    cover_image: Option<String>,
    health_score: Option<i16>,
    health_tags: Option<Vec<String>>,
}

#[derive(sqlx::FromRow)]
struct SimilarRecipe {
    id: Uuid,
    title: String,
    cover_image: Option<String>,
    common_tags: Option<i64>,
    similarity_score: Option<f64>,
}
