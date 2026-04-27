//! 菜谱业务逻辑层
//!
//! 在 handler 和 repo 之间做参数校验和业务编排。

use sqlx::PgPool;
use uuid::Uuid;
use validator::Validate;
use menu_common::error::AppError;
use crate::model::*;
use crate::repo::{RecipeRepo, PgRecipeRepo};

/// 营养信息数据库查询结果
#[derive(sqlx::FromRow)]
struct NutritionRow {
    calories: Option<rust_decimal::Decimal>,
    protein: Option<rust_decimal::Decimal>,
    fat: Option<rust_decimal::Decimal>,
    carbohydrate: Option<rust_decimal::Decimal>,
    fiber: Option<rust_decimal::Decimal>,
    sodium: Option<rust_decimal::Decimal>,
    health_score: Option<i16>,
    health_tags: Option<Vec<String>>,
    suitable_for: Option<Vec<String>>,
    analysis_text: Option<String>,
}

/// 菜谱服务，封装业务逻辑
pub struct RecipeService {
    repo: PgRecipeRepo,
}

impl RecipeService {
    pub fn new(pool: PgPool) -> Self {
        Self {
            repo: PgRecipeRepo::new(pool),
        }
    }

    /// 创建菜谱，校验请求参数
    pub async fn create(&self, author_id: Uuid, req: &CreateRecipeReq) -> Result<RecipeDetail, AppError> {
        req.validate()?;
        self.repo.create(author_id, req).await
    }

    /// 获取菜谱详情（含关联的食材、调料、步骤、标签、营养信息）
    pub async fn get_detail(&self, id: Uuid) -> Result<RecipeDetail, AppError> {
        let mut detail = self.repo
            .find_by_id(id)
            .await?
            .ok_or_else(|| AppError::NotFound("recipe not found".into()))?;

        // 尝试获取营养信息
        detail.nutrition = self.get_nutrition_info(id).await.ok();

        Ok(detail)
    }

    /// 获取营养信息（内部方法）
    async fn get_nutrition_info(&self, recipe_id: Uuid) -> Result<RecipeNutritionInfo, AppError> {
        let nutrition = sqlx::query_as::<_, NutritionRow>(
            r#"
            SELECT calories, protein, fat, carbohydrate, fiber, sodium,
                   health_score, health_tags, suitable_for, analysis_text
            FROM recipe_nutrition
            WHERE recipe_id = $1
            "#
        )
        .bind(recipe_id)
        .fetch_optional(self.repo.pool())
        .await
        .map_err(|e| AppError::Internal(e.into()))?
        .ok_or_else(|| AppError::NotFound("nutrition not found".into()))?;

        Ok(RecipeNutritionInfo {
            calories: nutrition.calories.map(|d| d.to_string().parse().unwrap_or(0.0)),
            protein: nutrition.protein.map(|d| d.to_string().parse().unwrap_or(0.0)),
            fat: nutrition.fat.map(|d| d.to_string().parse().unwrap_or(0.0)),
            carbohydrate: nutrition.carbohydrate.map(|d| d.to_string().parse().unwrap_or(0.0)),
            fiber: nutrition.fiber.map(|d| d.to_string().parse().unwrap_or(0.0)),
            sodium: nutrition.sodium.map(|d| d.to_string().parse().unwrap_or(0.0)),
            health_score: nutrition.health_score,
            health_tags: nutrition.health_tags,
            suitable_for: nutrition.suitable_for,
            analysis_text: nutrition.analysis_text,
        })
    }

    /// 更新菜谱，仅作者可操作
    pub async fn update(&self, id: Uuid, author_id: Uuid, req: &UpdateRecipeReq) -> Result<RecipeDetail, AppError> {
        req.validate()?;
        self.repo.update(id, author_id, req).await
    }

    /// 删除菜谱，仅作者可操作
    pub async fn delete(&self, id: Uuid, author_id: Uuid) -> Result<(), AppError> {
        self.repo.delete(id, author_id).await
    }

    /// 多条件搜索菜谱，返回分页数据
    pub async fn search(&self, params: &RecipeSearchParams) -> Result<(Vec<RecipeListItem>, i64), AppError> {
        self.repo.search(params).await
    }

    /// 随机推荐菜谱，默认 5 条，范围 [1, 20]
    pub async fn random(&self, count: Option<i64>) -> Result<Vec<RecipeListItem>, AppError> {
        self.repo.random(count.unwrap_or(5).clamp(1, 20)).await
    }
}
