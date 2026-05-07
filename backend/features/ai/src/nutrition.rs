use anyhow::{Context, Result};
use rust_decimal::Decimal;
use sqlx::PgPool;
use uuid::Uuid;

use crate::deepseek::DeepSeekClient;
use crate::model::RecipeNutrition;
use crate::repo::AiRepo;

pub struct NutritionService {
    repo: AiRepo,
    deepseek: DeepSeekClient,
}

impl NutritionService {
    pub fn new(pool: PgPool, api_key: String) -> Result<Self> {
        let deepseek = DeepSeekClient::new(api_key)?;
        Ok(Self {
            repo: AiRepo::new(pool),
            deepseek,
        })
    }

    /// 分析菜谱营养成分
    pub async fn analyze_recipe_nutrition(
        &self,
        recipe_id: Uuid,
        force: bool,
    ) -> Result<RecipeNutrition> {
        // 如果不强制重新分析，先检查缓存
        if !force && let Some(cached) = self.repo.get_nutrition(recipe_id).await? {
            tracing::info!("Using cached nutrition for recipe {}", recipe_id);
            return Ok(cached);
        }

        tracing::info!("Analyzing nutrition for recipe {}", recipe_id);

        // 获取菜谱详情
        let recipe_detail = self.fetch_recipe_detail(recipe_id).await?;

        // 格式化食材和调料
        let ingredients = self.format_ingredients(&recipe_detail.ingredients);
        let seasonings = self.format_seasonings(&recipe_detail.seasonings);
        let servings = recipe_detail.recipe.servings.unwrap_or(1);

        // 调用 DeepSeek API 分析
        let response = self
            .deepseek
            .analyze_nutrition(
                &recipe_detail.recipe.title,
                &ingredients,
                &seasonings,
                servings,
            )
            .await?;

        // 解析 AI 响应
        let nutrition = self.parse_nutrition_response(recipe_id, &response)?;

        // 保存到数据库
        self.repo.upsert_nutrition(&nutrition).await?;

        tracing::info!("Nutrition analysis completed for recipe {}", recipe_id);

        Ok(nutrition)
    }

    /// 获取菜谱详情（从 recipes 表）
    async fn fetch_recipe_detail(&self, recipe_id: Uuid) -> Result<RecipeDetailForAnalysis> {
        // 这里需要查询菜谱、食材、调料等信息
        // 为了简化，我们直接使用 SQL 查询
        let recipe =
            sqlx::query_as::<_, RecipeRow>("SELECT id, title, servings FROM recipes WHERE id = $1")
                .bind(recipe_id)
                .fetch_one(self.repo.pool())
                .await
                .context("Recipe not found")?;

        let ingredients = sqlx::query_as::<_, IngredientRow>(
            r#"
            SELECT ri.amount, ri.unit, i.name
            FROM recipe_ingredients ri
            JOIN ingredients i ON ri.ingredient_id = i.id
            WHERE ri.recipe_id = $1
            ORDER BY ri.sort_order
            "#,
        )
        .bind(recipe_id)
        .fetch_all(self.repo.pool())
        .await
        .context("Failed to fetch ingredients")?;

        let seasonings = sqlx::query_as::<_, SeasoningRow>(
            r#"
            SELECT rs.amount, rs.unit, s.name
            FROM recipe_seasonings rs
            JOIN seasonings s ON rs.seasoning_id = s.id
            WHERE rs.recipe_id = $1
            ORDER BY rs.sort_order
            "#,
        )
        .bind(recipe_id)
        .fetch_all(self.repo.pool())
        .await
        .context("Failed to fetch seasonings")?;

        Ok(RecipeDetailForAnalysis {
            recipe,
            ingredients,
            seasonings,
        })
    }

    /// 格式化食材列表
    fn format_ingredients(&self, ingredients: &[IngredientRow]) -> String {
        ingredients
            .iter()
            .map(|i| {
                if let Some(ref amount) = i.amount {
                    format!("{} {}", i.name, amount)
                } else {
                    format!("{} 适量", i.name)
                }
            })
            .collect::<Vec<_>>()
            .join("、")
    }

    /// 格式化调料列表
    fn format_seasonings(&self, seasonings: &[SeasoningRow]) -> String {
        seasonings
            .iter()
            .map(|s| {
                if let Some(ref amount) = s.amount {
                    format!("{} {}", s.name, amount)
                } else {
                    format!("{} 适量", s.name)
                }
            })
            .collect::<Vec<_>>()
            .join("、")
    }

    /// 解析 AI 响应为营养数据
    fn parse_nutrition_response(&self, recipe_id: Uuid, response: &str) -> Result<RecipeNutrition> {
        // 尝试提取 JSON 部分（AI 可能返回额外的文字）
        let json_str = if let Some(start) = response.find('{') {
            if let Some(end) = response.rfind('}') {
                &response[start..=end]
            } else {
                response
            }
        } else {
            response
        };

        let parsed: serde_json::Value =
            serde_json::from_str(json_str).context("Failed to parse AI response as JSON")?;

        Ok(RecipeNutrition {
            recipe_id,
            calories: parsed["calories"]
                .as_f64()
                .and_then(Decimal::from_f64_retain),
            protein: parsed["protein"]
                .as_f64()
                .and_then(Decimal::from_f64_retain),
            fat: parsed["fat"].as_f64().and_then(Decimal::from_f64_retain),
            carbohydrate: parsed["carbohydrate"]
                .as_f64()
                .and_then(Decimal::from_f64_retain),
            fiber: parsed["fiber"].as_f64().and_then(Decimal::from_f64_retain),
            sodium: parsed["sodium"].as_f64().and_then(Decimal::from_f64_retain),
            analysis_text: parsed["analysis_text"].as_str().map(String::from),
            health_score: parsed["health_score"].as_i64().map(|v| v as i16),
            health_tags: parsed["health_tags"].as_array().map(|arr| {
                arr.iter()
                    .filter_map(|v| v.as_str().map(String::from))
                    .collect()
            }),
            suitable_for: parsed["suitable_for"].as_array().map(|arr| {
                arr.iter()
                    .filter_map(|v| v.as_str().map(String::from))
                    .collect()
            }),
            cautions: parsed["cautions"].as_array().map(|arr| {
                arr.iter()
                    .filter_map(|v| v.as_str().map(String::from))
                    .collect()
            }),
            serving_size: parsed["serving_size"].as_str().map(String::from),
            traffic_light: parsed.get("traffic_light").cloned(),
            overall_rating: parsed["overall_rating"].as_str().map(String::from),
            summary: parsed["summary"].as_str().map(String::from),
            generated_at: None,
            updated_at: None,
        })
    }
}

// 辅助结构体
#[derive(sqlx::FromRow)]
#[allow(dead_code)]
struct RecipeRow {
    id: Uuid,
    title: String,
    servings: Option<i16>,
}

#[derive(sqlx::FromRow)]
#[allow(dead_code)]
struct IngredientRow {
    name: String,
    amount: Option<String>,
    unit: Option<String>,
}

#[derive(sqlx::FromRow)]
#[allow(dead_code)]
struct SeasoningRow {
    name: String,
    amount: Option<String>,
    unit: Option<String>,
}

struct RecipeDetailForAnalysis {
    recipe: RecipeRow,
    ingredients: Vec<IngredientRow>,
    seasonings: Vec<SeasoningRow>,
}
