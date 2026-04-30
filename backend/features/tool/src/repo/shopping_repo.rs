//! 购物清单数据访问层
//!
//! 定义 ShoppingRepo trait 和 PostgreSQL 实现，
//! 管理购物清单及其清单项的 CRUD 操作，所有操作校验用户归属权。

use crate::model::*;
use async_trait::async_trait;
use culino_common::error::AppError;
use sqlx::PgPool;
use uuid::Uuid;

/// 购物清单仓储接口
#[async_trait]
pub trait ShoppingRepo: Send + Sync {
    /// 查询用户的所有购物清单
    async fn list_by_user(&self, user_id: Uuid) -> Result<Vec<ShoppingList>, AppError>;
    /// 创建购物清单
    async fn create(
        &self,
        user_id: Uuid,
        req: &CreateShoppingListReq,
    ) -> Result<ShoppingList, AppError>;
    /// 查询购物清单详情（含清单项）
    async fn find_by_id(
        &self,
        id: Uuid,
        user_id: Uuid,
    ) -> Result<Option<ShoppingListDetail>, AppError>;
    /// 删除购物清单
    async fn delete(&self, id: Uuid, user_id: Uuid) -> Result<(), AppError>;
    /// 向购物清单添加商品项
    async fn add_item(
        &self,
        list_id: Uuid,
        req: &AddShoppingItemReq,
    ) -> Result<ShoppingListItem, AppError>;
    /// 更新购物清单项
    async fn update_item(
        &self,
        item_id: i32,
        list_id: Uuid,
        req: &UpdateShoppingItemReq,
    ) -> Result<ShoppingListItem, AppError>;
    /// 删除购物清单项
    async fn delete_item(&self, item_id: i32, list_id: Uuid) -> Result<(), AppError>;
}

/// PostgreSQL 购物清单仓储实现
pub struct PgShoppingRepo {
    pool: PgPool,
}

impl PgShoppingRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl ShoppingRepo for PgShoppingRepo {
    /// 查询用户的所有购物清单，按创建时间倒序
    async fn list_by_user(&self, user_id: Uuid) -> Result<Vec<ShoppingList>, AppError> {
        let rows = sqlx::query_as::<_, ShoppingList>(
            "SELECT * FROM shopping_lists WHERE user_id = $1 ORDER BY created_at DESC",
        )
        .bind(user_id)
        .fetch_all(&self.pool)
        .await?;
        Ok(rows)
    }

    async fn create(
        &self,
        user_id: Uuid,
        req: &CreateShoppingListReq,
    ) -> Result<ShoppingList, AppError> {
        let row = sqlx::query_as::<_, ShoppingList>(
            "INSERT INTO shopping_lists (user_id, title) VALUES ($1, $2) RETURNING *",
        )
        .bind(user_id)
        .bind(&req.title)
        .fetch_one(&self.pool)
        .await?;
        Ok(row)
    }

    /// 查询购物清单详情（含清单项），同时校验用户归属权
    async fn find_by_id(
        &self,
        id: Uuid,
        user_id: Uuid,
    ) -> Result<Option<ShoppingListDetail>, AppError> {
        let list = match sqlx::query_as::<_, ShoppingList>(
            "SELECT * FROM shopping_lists WHERE id = $1 AND user_id = $2",
        )
        .bind(id)
        .bind(user_id)
        .fetch_optional(&self.pool)
        .await?
        {
            Some(l) => l,
            None => return Ok(None),
        };

        let items = sqlx::query_as::<_, ShoppingListItem>(
            "SELECT * FROM shopping_list_items WHERE list_id = $1 ORDER BY sort_order",
        )
        .bind(id)
        .fetch_all(&self.pool)
        .await?;

        Ok(Some(ShoppingListDetail { list, items }))
    }

    /// 删除购物清单，仅允许删除自己的清单
    async fn delete(&self, id: Uuid, user_id: Uuid) -> Result<(), AppError> {
        let result = sqlx::query("DELETE FROM shopping_lists WHERE id = $1 AND user_id = $2")
            .bind(id)
            .bind(user_id)
            .execute(&self.pool)
            .await?;
        if result.rows_affected() == 0 {
            return Err(AppError::NotFound("shopping list not found".into()));
        }
        Ok(())
    }

    async fn add_item(
        &self,
        list_id: Uuid,
        req: &AddShoppingItemReq,
    ) -> Result<ShoppingListItem, AppError> {
        let row = sqlx::query_as::<_, ShoppingListItem>(
            "INSERT INTO shopping_list_items (list_id, name, amount, sort_order) VALUES ($1,$2,$3,$4) RETURNING *",
        )
        .bind(list_id)
        .bind(&req.name)
        .bind(&req.amount)
        .bind(req.sort_order)
        .fetch_one(&self.pool)
        .await?;
        Ok(row)
    }

    /// 更新购物清单项，使用 COALESCE 实现部分更新
    async fn update_item(
        &self,
        item_id: i32,
        list_id: Uuid,
        req: &UpdateShoppingItemReq,
    ) -> Result<ShoppingListItem, AppError> {
        let row = sqlx::query_as::<_, ShoppingListItem>(
            "UPDATE shopping_list_items SET name = COALESCE($2, name), amount = COALESCE($3, amount), is_checked = COALESCE($4, is_checked), sort_order = COALESCE($5, sort_order) WHERE id = $1 AND list_id = $6 RETURNING *",
        )
        .bind(item_id)
        .bind(&req.name)
        .bind(&req.amount)
        .bind(req.is_checked)
        .bind(req.sort_order)
        .bind(list_id)
        .fetch_optional(&self.pool)
        .await?
        .ok_or_else(|| AppError::NotFound("shopping list item not found".into()))?;
        Ok(row)
    }

    async fn delete_item(&self, item_id: i32, list_id: Uuid) -> Result<(), AppError> {
        let result = sqlx::query("DELETE FROM shopping_list_items WHERE id = $1 AND list_id = $2")
            .bind(item_id)
            .bind(list_id)
            .execute(&self.pool)
            .await?;
        if result.rows_affected() == 0 {
            return Err(AppError::NotFound("shopping list item not found".into()));
        }
        Ok(())
    }
}
