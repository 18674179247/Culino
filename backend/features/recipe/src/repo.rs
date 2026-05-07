//! 菜谱数据访问层
//!
//! 定义 RecipeRepo trait 和 PostgreSQL 实现。
//! 负责菜谱及其关联数据（食材、调料、步骤、标签）的数据库操作。
//! 创建和更新操作使用事务保证数据一致性。

use anyhow::Context;
use async_trait::async_trait;
use culino_common::error::AppError;
use culino_common::pagination::paginate_sql;
use culino_common::tx::with_tx;
use sqlx::PgPool;
use uuid::Uuid;

use crate::model::*;

/// 菜谱仓储接口
#[async_trait]
pub trait RecipeRepo: Send + Sync {
    /// 创建菜谱及其关联数据
    async fn create(
        &self,
        author_id: Uuid,
        req: &CreateRecipeReq,
    ) -> Result<RecipeDetail, AppError>;
    /// 根据 ID 查找菜谱详情
    async fn find_by_id(&self, id: Uuid) -> Result<Option<RecipeDetail>, AppError>;
    /// 更新菜谱及其关联数据
    async fn update(
        &self,
        id: Uuid,
        author_id: Uuid,
        req: &UpdateRecipeReq,
    ) -> Result<RecipeDetail, AppError>;
    /// 删除菜谱
    async fn delete(&self, id: Uuid, author_id: Uuid) -> Result<(), AppError>;
    /// 多条件搜索菜谱
    async fn search(
        &self,
        params: &RecipeSearchParams,
    ) -> Result<(Vec<RecipeListItem>, i64), AppError>;
    /// 随机获取已发布的菜谱
    async fn random(&self, count: i64) -> Result<Vec<RecipeListItem>, AppError>;
}

/// PostgreSQL 菜谱仓储
pub struct PgRecipeRepo {
    pool: PgPool,
}

impl PgRecipeRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl RecipeRepo for PgRecipeRepo {
    /// 在事务中创建菜谱及其关联的食材、调料、步骤、标签
    async fn create(
        &self,
        author_id: Uuid,
        req: &CreateRecipeReq,
    ) -> Result<RecipeDetail, AppError> {
        let pool = &self.pool;
        let recipe_id = with_tx(pool, |mut tx| Box::pin(async move {
            // 插入菜谱主表
            let recipe = sqlx::query_as::<_, Recipe>(
                "INSERT INTO recipes (title, description, cover_image, difficulty, cooking_time, prep_time, servings, source, author_id) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9) RETURNING *"
            )
                .bind(&req.title)
                .bind(&req.description)
                .bind(&req.cover_image)
                .bind(req.difficulty)
                .bind(req.cooking_time)
                .bind(req.prep_time)
                .bind(req.servings)
                .bind(&req.source)
                .bind(author_id)
                .fetch_one(&mut *tx)
                .await?;

            let recipe_id = recipe.id;

            // 批量插入菜谱食材
            if let Some(ref items) = req.ingredients {
                for item in items {
                    let ingredient_id = match item.ingredient_id {
                        Some(id) => id,
                        None => {
                            let name = item.name.as_deref().unwrap_or("未知食材");
                            let row = sqlx::query_scalar::<_, i32>(
                                "INSERT INTO ingredients (name) VALUES ($1) ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id"
                            )
                                .bind(name)
                                .fetch_one(&mut *tx)
                                .await?;
                            row
                        }
                    };
                    sqlx::query("INSERT INTO recipe_ingredients (recipe_id, ingredient_id, amount, unit, note, sort_order) VALUES ($1,$2,$3,$4,$5,$6)")
                        .bind(recipe_id)
                        .bind(ingredient_id)
                        .bind(&item.amount)
                        .bind(&item.unit)
                        .bind(&item.note)
                        .bind(item.sort_order)
                        .execute(&mut *tx)
                        .await?;
                }
            }

            // 批量插入菜谱调料
            if let Some(ref items) = req.seasonings {
                for item in items {
                    let seasoning_id = match item.seasoning_id {
                        Some(id) => id,
                        None => {
                            let name = item.name.as_deref().unwrap_or("未知调料");
                            let row = sqlx::query_scalar::<_, i32>(
                                "INSERT INTO seasonings (name) VALUES ($1) ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id"
                            )
                                .bind(name)
                                .fetch_one(&mut *tx)
                                .await?;
                            row
                        }
                    };
                    sqlx::query("INSERT INTO recipe_seasonings (recipe_id, seasoning_id, amount, unit, sort_order) VALUES ($1,$2,$3,$4,$5)")
                        .bind(recipe_id)
                        .bind(seasoning_id)
                        .bind(&item.amount)
                        .bind(&item.unit)
                        .bind(item.sort_order)
                        .execute(&mut *tx)
                        .await?;
                }
            }

            // 批量插入菜谱步骤
            if let Some(ref items) = req.steps {
                for item in items {
                    sqlx::query("INSERT INTO recipe_steps (recipe_id, step_number, content, image, duration) VALUES ($1,$2,$3,$4,$5)")
                        .bind(recipe_id)
                        .bind(item.step_number)
                        .bind(&item.content)
                        .bind(&item.image)
                        .bind(item.duration)
                        .execute(&mut *tx)
                        .await?;
                }
            }

            // 批量插入菜谱标签
            if let Some(ref tag_ids) = req.tag_ids {
                for tag_id in tag_ids {
                    sqlx::query("INSERT INTO recipe_tags (recipe_id, tag_id) VALUES ($1, $2)")
                        .bind(recipe_id)
                        .bind(tag_id)
                        .execute(&mut *tx)
                        .await?;
                }
            }

            Ok((recipe_id, tx))
        })).await?;

        // 查询完整详情返回
        Ok(self
            .find_by_id(recipe_id)
            .await?
            .context("recipe created but not found")?)
    }

    async fn find_by_id(&self, id: Uuid) -> Result<Option<RecipeDetail>, AppError> {
        let recipe = match sqlx::query_as::<_, Recipe>("SELECT * FROM recipes WHERE id = $1")
            .bind(id)
            .fetch_optional(&self.pool)
            .await?
        {
            Some(r) => r,
            None => return Ok(None),
        };

        let ingredients = sqlx::query_as::<_, RecipeIngredient>(
            "SELECT ri.id, ri.recipe_id, ri.ingredient_id, i.name as ingredient_name, ri.amount, ri.unit, ri.note, ri.sort_order FROM recipe_ingredients ri JOIN ingredients i ON i.id = ri.ingredient_id WHERE ri.recipe_id = $1 ORDER BY ri.sort_order",
        )
            .bind(id)
            .fetch_all(&self.pool)
            .await?;

        let seasonings = sqlx::query_as::<_, RecipeSeasoning>(
            "SELECT rs.id, rs.recipe_id, rs.seasoning_id, s.name as seasoning_name, rs.amount, rs.unit, rs.sort_order FROM recipe_seasonings rs JOIN seasonings s ON s.id = rs.seasoning_id WHERE rs.recipe_id = $1 ORDER BY rs.sort_order",
        )
            .bind(id)
            .fetch_all(&self.pool)
            .await?;

        let steps = sqlx::query_as::<_, RecipeStep>(
            "SELECT * FROM recipe_steps WHERE recipe_id = $1 ORDER BY step_number",
        )
            .bind(id)
            .fetch_all(&self.pool)
            .await?;

        let tags = sqlx::query_as::<_, RecipeTag>("SELECT rt.recipe_id, rt.tag_id, t.name as tag_name FROM recipe_tags rt JOIN tags t ON t.id = rt.tag_id WHERE rt.recipe_id = $1")
            .bind(id)
            .fetch_all(&self.pool)
            .await?;

        Ok(Some(RecipeDetail {
            recipe,
            ingredients,
            seasonings,
            steps,
            tags,
            nutrition: None, // 营养信息由 service 层填充
            author: None,    // 作者信息由 service 层填充
            like_count: None,
            comment_count: None,
        }))
    }

    /// 更新菜谱主表 + 全量替换关联数据（先删后插）
    async fn update(
        &self,
        id: Uuid,
        author_id: Uuid,
        req: &UpdateRecipeReq,
    ) -> Result<RecipeDetail, AppError> {
        let pool = &self.pool;
        let recipe_id = with_tx(pool, |mut tx| Box::pin(async move {
            let recipe = sqlx::query_as::<_, Recipe>(
                "UPDATE recipes SET title = COALESCE($3, title), description = COALESCE($4, description), cover_image = COALESCE($5, cover_image), difficulty = COALESCE($6, difficulty), cooking_time = COALESCE($7, cooking_time), prep_time = COALESCE($8, prep_time), servings = COALESCE($9, servings), source = COALESCE($10, source), updated_at = now() WHERE id = $1 AND author_id = $2 RETURNING *"
            )
                .bind(id)
                .bind(author_id)
                .bind(&req.title)
                .bind(&req.description)
                .bind(&req.cover_image)
                .bind(req.difficulty)
                .bind(req.cooking_time)
                .bind(req.prep_time)
                .bind(req.servings)
                .bind(&req.source)
                .fetch_optional(&mut *tx)
                .await?
                .ok_or_else(|| AppError::NotFound("recipe not found or not owned by you".into()))?;

            let recipe_id = recipe.id;

            // 替换食材
            if let Some(ref items) = req.ingredients {
                sqlx::query("DELETE FROM recipe_ingredients WHERE recipe_id = $1")
                    .bind(recipe_id)
                    .execute(&mut *tx)
                    .await?;
                for item in items {
                    let ingredient_id = match item.ingredient_id {
                        Some(id) => id,
                        None => {
                            let name = item.name.as_deref().unwrap_or("未知食材");
                            sqlx::query_scalar::<_, i32>(
                                "INSERT INTO ingredients (name) VALUES ($1) ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id"
                            )
                                .bind(name)
                                .fetch_one(&mut *tx)
                                .await?
                        }
                    };
                    sqlx::query("INSERT INTO recipe_ingredients (recipe_id, ingredient_id, amount, unit, note, sort_order) VALUES ($1,$2,$3,$4,$5,$6)")
                        .bind(recipe_id)
                        .bind(ingredient_id)
                        .bind(&item.amount)
                        .bind(&item.unit)
                        .bind(&item.note)
                        .bind(item.sort_order)
                        .execute(&mut *tx)
                        .await?;
                }
            }

            // 替换调料
            if let Some(ref items) = req.seasonings {
                sqlx::query("DELETE FROM recipe_seasonings WHERE recipe_id = $1")
                    .bind(recipe_id)
                    .execute(&mut *tx)
                    .await?;
                for item in items {
                    let seasoning_id = match item.seasoning_id {
                        Some(id) => id,
                        None => {
                            let name = item.name.as_deref().unwrap_or("未知调料");
                            sqlx::query_scalar::<_, i32>(
                                "INSERT INTO seasonings (name) VALUES ($1) ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id"
                            )
                                .bind(name)
                                .fetch_one(&mut *tx)
                                .await?
                        }
                    };
                    sqlx::query("INSERT INTO recipe_seasonings (recipe_id, seasoning_id, amount, unit, sort_order) VALUES ($1,$2,$3,$4,$5)")
                        .bind(recipe_id)
                        .bind(seasoning_id)
                        .bind(&item.amount)
                        .bind(&item.unit)
                        .bind(item.sort_order)
                        .execute(&mut *tx)
                        .await?;
                }
            }

            // 替换步骤
            if let Some(ref items) = req.steps {
                sqlx::query("DELETE FROM recipe_steps WHERE recipe_id = $1")
                    .bind(recipe_id)
                    .execute(&mut *tx)
                    .await?;
                for item in items {
                    sqlx::query("INSERT INTO recipe_steps (recipe_id, step_number, content, image, duration) VALUES ($1,$2,$3,$4,$5)")
                        .bind(recipe_id)
                        .bind(item.step_number)
                        .bind(&item.content)
                        .bind(&item.image)
                        .bind(item.duration)
                        .execute(&mut *tx)
                        .await?;
                }
            }

            // 替换标签
            if let Some(ref tag_ids) = req.tag_ids {
                sqlx::query("DELETE FROM recipe_tags WHERE recipe_id = $1")
                    .bind(recipe_id)
                    .execute(&mut *tx)
                    .await?;
                for tag_id in tag_ids {
                    sqlx::query("INSERT INTO recipe_tags (recipe_id, tag_id) VALUES ($1, $2)")
                        .bind(recipe_id)
                        .bind(tag_id)
                        .execute(&mut *tx)
                        .await?;
                }
            }

            Ok((recipe_id, tx))
        })).await?;

        Ok(self
            .find_by_id(recipe_id)
            .await?
            .context("recipe updated but not found")?)
    }

    async fn delete(&self, id: Uuid, author_id: Uuid) -> Result<(), AppError> {
        let result = sqlx::query("DELETE FROM recipes WHERE id = $1 AND author_id = $2")
            .bind(id)
            .bind(author_id)
            .execute(&self.pool)
            .await?;
        if result.rows_affected() == 0 {
            return Err(AppError::NotFound(
                "recipe not found or not owned by you".into(),
            ));
        }
        Ok(())
    }

    async fn search(
        &self,
        params: &RecipeSearchParams,
    ) -> Result<(Vec<RecipeListItem>, i64), AppError> {
        // 动态构建 WHERE 子句
        let mut conditions = vec!["status = 1".to_string()];
        let mut param_index = 1u32;

        // 解析逗号分隔的 tag_ids
        let tag_ids: Vec<i32> = params
            .tag_ids
            .as_deref()
            .unwrap_or("")
            .split(',')
            .filter_map(|s| s.trim().parse().ok())
            .collect();
        let has_tags = !tag_ids.is_empty();

        if has_tags {
            conditions.push(format!(
                "id IN (SELECT recipe_id FROM recipe_tags WHERE tag_id = ANY(${param_index}))"
            ));
            param_index += 1;
        }

        // 解析逗号分隔的 ingredient_ids
        let ingredient_ids: Vec<i32> = params
            .ingredient_ids
            .as_deref()
            .unwrap_or("")
            .split(',')
            .filter_map(|s| s.trim().parse().ok())
            .collect();
        let has_ingredients = !ingredient_ids.is_empty();

        if has_ingredients {
            conditions.push(format!(
                "id IN (SELECT recipe_id FROM recipe_ingredients WHERE ingredient_id = ANY(${param_index}))"
            ));
            param_index += 1;
        }

        let has_keyword = params.keyword.as_ref().is_some_and(|k| !k.is_empty());
        if has_keyword {
            conditions.push(format!(
                "(title ILIKE '%' || ${param_index} || '%' OR description ILIKE '%' || ${param_index} || '%')"
            ));
            param_index += 1;
        }

        if params.difficulty.is_some() {
            conditions.push(format!("difficulty = ${param_index}"));
            param_index += 1;
        }

        if params.max_cooking_time.is_some() {
            conditions.push(format!("cooking_time <= ${param_index}"));
            param_index += 1;
        }

        if params.author_id.is_some() {
            conditions.push(format!("author_id = ${param_index}"));
            param_index += 1;
        }

        let where_clause = conditions.join(" AND ");

        let page = params.page.unwrap_or(1).max(1);
        let page_size = params.page_size.unwrap_or(20).clamp(1, 100);
        let offset = (page - 1) * page_size;

        // 单次查询：用 COUNT(*) OVER() 同时获取数据和总数
        let base_sql = format!(
            "SELECT id, title, description, cover_image, difficulty, cooking_time, servings, author_id, created_at FROM recipes WHERE {where_clause}"
        );
        let data_sql = paginate_sql(
            &base_sql,
            "_inner.created_at DESC",
            param_index,
            param_index + 1,
        )?;

        let mut query = sqlx::query_as::<_, RecipeListItemCounted>(&data_sql);
        if has_tags {
            query = query.bind(&tag_ids);
        }
        if has_ingredients {
            query = query.bind(&ingredient_ids);
        }
        if has_keyword {
            query = query.bind(params.keyword.as_deref().unwrap_or(""));
        }
        if let Some(d) = params.difficulty {
            query = query.bind(d);
        }
        if let Some(t) = params.max_cooking_time {
            query = query.bind(t);
        }
        if let Some(a) = params.author_id {
            query = query.bind(a);
        }
        query = query.bind(page_size).bind(offset);

        let rows = query.fetch_all(&self.pool).await?;
        let total = rows.first().map(|r| r.total_count).unwrap_or(0);
        let items = rows.into_iter().map(|r| r.into_item()).collect();

        Ok((items, total))
    }

    async fn random(&self, count: i64) -> Result<Vec<RecipeListItem>, AppError> {
        // 先查询总数
        let total: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM recipes WHERE status = 1")
            .fetch_one(&self.pool)
            .await?;

        // 如果总数较少(< 100),直接使用 ORDER BY random()
        // 如果总数较多,使用两阶段随机以提高性能
        let items = if total < 100 {
            sqlx::query_as::<_, RecipeListItem>(
                "SELECT id, title, description, cover_image, difficulty, cooking_time, servings, author_id, created_at \
                 FROM recipes \
                 WHERE status = 1 \
                 ORDER BY random() \
                 LIMIT $1"
            )
                .bind(count)
                .fetch_all(&self.pool)
                .await?
        } else {
            // 大数据量时,先通过 random() 阈值筛选约 10% 的行,再从中随机排序取 N 条
            sqlx::query_as::<_, RecipeListItem>(
                "SELECT id, title, description, cover_image, difficulty, cooking_time, servings, author_id, created_at \
                 FROM recipes \
                 WHERE status = 1 AND random() < 0.1 \
                 ORDER BY random() \
                 LIMIT $1"
            )
                .bind(count)
                .fetch_all(&self.pool)
                .await?
        };
        Ok(items)
    }
}
