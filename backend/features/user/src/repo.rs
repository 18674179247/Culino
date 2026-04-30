//! 用户数据访问层
//!
//! 定义 UserRepo trait 和 PostgreSQL 实现，
//! 负责用户、角色、权限的数据库操作。

use async_trait::async_trait;
use sqlx::PgPool;
use uuid::Uuid;

use culino_common::error::AppError;

use crate::model::{CreateUser, Permission, Role, UpdateUser, User};

/// 用户仓储接口
#[async_trait]
pub trait UserRepo: Send + Sync {
    /// 根据 ID 查找用户
    async fn find_by_id(&self, id: Uuid) -> Result<Option<User>, AppError>;
    /// 根据用户名查找用户
    async fn find_by_username(&self, username: &str) -> Result<Option<User>, AppError>;
    /// 创建新用户
    async fn create(&self, user: &CreateUser) -> Result<User, AppError>;
    /// 更新用户信息
    async fn update(&self, id: Uuid, data: &UpdateUser) -> Result<User, AppError>;
    /// 根据 ID 查找角色
    async fn find_role_by_id(&self, role_id: i32) -> Result<Option<Role>, AppError>;
    /// 查询角色关联的权限列表
    async fn find_permissions(&self, role_id: i32) -> Result<Vec<Permission>, AppError>;
}

/// PostgreSQL 用户仓储实现
pub struct PgUserRepo {
    pool: PgPool,
}

impl PgUserRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl UserRepo for PgUserRepo {
    async fn find_by_id(&self, id: Uuid) -> Result<Option<User>, AppError> {
        let user = sqlx::query_as::<_, User>("SELECT * FROM users WHERE id = $1")
            .bind(id)
            .fetch_optional(&self.pool)
            .await?;
        Ok(user)
    }

    async fn find_by_username(&self, username: &str) -> Result<Option<User>, AppError> {
        let user = sqlx::query_as::<_, User>("SELECT * FROM users WHERE username = $1")
            .bind(username)
            .fetch_optional(&self.pool)
            .await?;
        Ok(user)
    }

    /// 创建用户，用户名唯一约束冲突时返回 Conflict 错误
    async fn create(&self, data: &CreateUser) -> Result<User, AppError> {
        let user = sqlx::query_as::<_, User>(
            "INSERT INTO users (username, nickname, password_hash) VALUES ($1, $2, $3) RETURNING *",
        )
        .bind(&data.username)
        .bind(&data.nickname)
        .bind(&data.password_hash)
        .fetch_one(&self.pool)
        .await
        .map_err(|e| match e {
            sqlx::Error::Database(ref db_err) if db_err.is_unique_violation() => {
                AppError::Conflict("username already exists".into())
            }
            _ => AppError::from(e),
        })?;
        Ok(user)
    }

    /// 使用 COALESCE 实现部分更新，仅更新非 None 字段
    async fn update(&self, id: Uuid, data: &UpdateUser) -> Result<User, AppError> {
        let user = sqlx::query_as::<_, User>(
            "UPDATE users SET nickname = COALESCE($2, nickname), avatar = COALESCE($3, avatar), updated_at = now() WHERE id = $1 RETURNING *",
        )
        .bind(id)
        .bind(&data.nickname)
        .bind(&data.avatar)
        .fetch_one(&self.pool)
        .await?;
        Ok(user)
    }

    async fn find_role_by_id(&self, role_id: i32) -> Result<Option<Role>, AppError> {
        let role = sqlx::query_as::<_, Role>("SELECT * FROM roles WHERE id = $1")
            .bind(role_id)
            .fetch_optional(&self.pool)
            .await?;
        Ok(role)
    }

    /// 通过角色-权限关联表查询权限列表
    async fn find_permissions(&self, role_id: i32) -> Result<Vec<Permission>, AppError> {
        let perms = sqlx::query_as::<_, Permission>(
            "SELECT p.* FROM permissions p JOIN role_permissions rp ON p.id = rp.permission_id WHERE rp.role_id = $1",
        )
        .bind(role_id)
        .fetch_all(&self.pool)
        .await?;
        Ok(perms)
    }
}
