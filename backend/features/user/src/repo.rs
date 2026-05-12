//! 用户数据访问层
//!
//! 定义 UserRepo / InviteCodeRepo trait 和 PostgreSQL 实现。

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use sqlx::PgPool;
use uuid::Uuid;

use culino_common::error::AppError;

use crate::model::{CreateUser, InviteCode, Permission, Role, UpdateUser, User};

/// 用户仓储接口
#[async_trait]
pub trait UserRepo: Send + Sync {
    /// 根据 ID 查找用户
    async fn find_by_id(&self, id: Uuid) -> Result<Option<User>, AppError>;
    /// 根据用户名查找用户
    async fn find_by_username(&self, username: &str) -> Result<Option<User>, AppError>;
    /// 在事务中消费邀请码并创建新用户，两步原子完成
    async fn create_with_invite(&self, user: &CreateUser) -> Result<User, AppError>;
    /// 更新用户信息
    async fn update(&self, id: Uuid, data: &UpdateUser) -> Result<User, AppError>;
    /// 根据 ID 查找角色
    async fn find_role_by_id(&self, role_id: i32) -> Result<Option<Role>, AppError>;
    /// 查询角色关联的权限列表
    async fn find_permissions(&self, role_id: i32) -> Result<Vec<Permission>, AppError>;
}

/// 邀请码仓储接口
#[async_trait]
pub trait InviteCodeRepo: Send + Sync {
    async fn create(
        &self,
        code: &str,
        created_by: Uuid,
        max_uses: i32,
        expires_at: Option<DateTime<Utc>>,
        note: Option<&str>,
    ) -> Result<InviteCode, AppError>;
    async fn list(&self) -> Result<Vec<InviteCode>, AppError>;
    async fn revoke(&self, code: &str) -> Result<(), AppError>;
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

    /// 事务中完成：
    /// 1. SELECT ... FOR UPDATE 锁住邀请码行
    /// 2. 校验过期、剩余次数
    /// 3. used_count += 1
    /// 4. 插入 users
    /// 任一步失败整体回滚，避免并发下邀请码被超额使用。
    async fn create_with_invite(&self, data: &CreateUser) -> Result<User, AppError> {
        let mut tx = self.pool.begin().await?;

        let invite: Option<InviteCode> = sqlx::query_as::<_, InviteCode>(
            "SELECT * FROM invite_codes WHERE code = $1 FOR UPDATE",
        )
        .bind(&data.invited_by)
        .fetch_optional(&mut *tx)
        .await?;

        let invite = invite.ok_or_else(|| AppError::BadRequest("invalid invite code".into()))?;

        if invite.expires_at.is_some_and(|exp| exp < Utc::now()) {
            return Err(AppError::BadRequest("invite code expired".into()));
        }
        if invite.used_count >= invite.max_uses {
            return Err(AppError::BadRequest("invite code exhausted".into()));
        }

        sqlx::query("UPDATE invite_codes SET used_count = used_count + 1 WHERE code = $1")
            .bind(&data.invited_by)
            .execute(&mut *tx)
            .await?;

        let user = sqlx::query_as::<_, User>(
            "INSERT INTO users (username, nickname, password_hash, invited_by) \
             VALUES ($1, $2, $3, $4) RETURNING *",
        )
        .bind(&data.username)
        .bind(&data.nickname)
        .bind(&data.password_hash)
        .bind(&data.invited_by)
        .fetch_one(&mut *tx)
        .await
        .map_err(|e| match e {
            sqlx::Error::Database(ref db_err) if db_err.is_unique_violation() => {
                AppError::Conflict("username already exists".into())
            }
            _ => AppError::from(e),
        })?;

        tx.commit().await?;
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

/// 邀请码仓储实现
pub struct PgInviteCodeRepo {
    pool: PgPool,
}

impl PgInviteCodeRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl InviteCodeRepo for PgInviteCodeRepo {
    async fn create(
        &self,
        code: &str,
        created_by: Uuid,
        max_uses: i32,
        expires_at: Option<DateTime<Utc>>,
        note: Option<&str>,
    ) -> Result<InviteCode, AppError> {
        let row = sqlx::query_as::<_, InviteCode>(
            "INSERT INTO invite_codes (code, created_by, max_uses, expires_at, note) \
             VALUES ($1, $2, $3, $4, $5) RETURNING *",
        )
        .bind(code)
        .bind(created_by)
        .bind(max_uses)
        .bind(expires_at)
        .bind(note)
        .fetch_one(&self.pool)
        .await?;
        Ok(row)
    }

    async fn list(&self) -> Result<Vec<InviteCode>, AppError> {
        let rows =
            sqlx::query_as::<_, InviteCode>("SELECT * FROM invite_codes ORDER BY created_at DESC")
                .fetch_all(&self.pool)
                .await?;
        Ok(rows)
    }

    async fn revoke(&self, code: &str) -> Result<(), AppError> {
        let result = sqlx::query("DELETE FROM invite_codes WHERE code = $1")
            .bind(code)
            .execute(&self.pool)
            .await?;
        if result.rows_affected() == 0 {
            return Err(AppError::NotFound("invite code not found".into()));
        }
        Ok(())
    }
}
