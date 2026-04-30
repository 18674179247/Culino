//! 数据库事务辅助
//!
//! 提供 `with_tx` 函数，自动管理事务的 begin/commit/rollback。

use sqlx::PgPool;

use crate::error::AppError;

/// 在事务中执行操作，成功时自动 commit，失败时 tx drop 自动 rollback。
///
/// 调用方式：
/// ```ignore
/// with_tx(&pool, |mut tx| Box::pin(async move {
///     // ... 使用 &mut *tx 执行查询 ...
///     Ok((result, tx))
/// })).await?;
/// ```
pub async fn with_tx<'a, T, F>(pool: &PgPool, f: F) -> Result<T, AppError>
where
    T: Send + 'a,
    F: FnOnce(
        sqlx::Transaction<'a, sqlx::Postgres>,
    ) -> std::pin::Pin<
        Box<
            dyn std::future::Future<
                    Output = Result<(T, sqlx::Transaction<'a, sqlx::Postgres>), AppError>,
                > + Send
                + 'a,
        >,
    >,
{
    let tx = pool.begin().await?;
    let (result, tx) = f(tx).await?;
    tx.commit().await?;
    Ok(result)
}
