//! 分页工具
//!
//! 提供通用的分页参数解析和分页响应结构。

use serde::Deserialize;

/// 分页查询参数
#[derive(Debug, Deserialize, utoipa::IntoParams)]
pub struct PaginationParams {
    /// 页码，从 1 开始，默认 1
    pub page: Option<i64>,
    /// 每页条数，默认 20，最大 100
    pub page_size: Option<i64>,
}

impl PaginationParams {
    /// 计算 SQL OFFSET 值
    pub fn offset(&self) -> i64 {
        (self.page() - 1) * self.limit()
    }

    /// 获取每页条数（即 SQL LIMIT），范围 [1, 100]
    pub fn limit(&self) -> i64 {
        self.page_size.unwrap_or(20).clamp(1, 100)
    }

    /// 获取当前页码，最小为 1
    pub fn page(&self) -> i64 {
        self.page.unwrap_or(1).max(1)
    }
}

/// 分页响应包装器
#[derive(Debug, serde::Serialize, utoipa::ToSchema)]
pub struct PaginatedResponse<T: serde::Serialize> {
    /// 当前页数据列表
    pub data: Vec<T>,
    /// 总记录数
    pub total: i64,
    /// 当前页码
    pub page: i64,
    /// 每页条数
    pub page_size: i64,
}

/// 将基础 SQL 包装为带 `COUNT(*) OVER() AS total_count` 的单次分页查询
///
/// `order_by` 参数会直接拼接到 SQL 中，调用方必须传入硬编码字符串，
/// 绝对不能传入用户输入，否则会导致 SQL 注入。
///
/// 为防范误用，此函数运行时检查 `order_by` 是否包含 `;` 或 `--`。
///
/// 生成形如：
/// ```sql
/// SELECT *, COUNT(*) OVER() AS total_count
/// FROM (base_sql) AS _inner
/// ORDER BY _inner.created_at DESC
/// LIMIT $N OFFSET $M
/// ```
pub fn paginate_sql(
    base_sql: &str,
    order_by: &str,
    limit_param: u32,
    offset_param: u32,
) -> Result<String, crate::error::AppError> {
    if order_by.contains(';') || order_by.contains("--") {
        return Err(crate::error::AppError::Internal(anyhow::anyhow!(
            "order_by 参数包含非法字符"
        )));
    }
    Ok(format!(
        "SELECT *, COUNT(*) OVER() AS total_count FROM ({base_sql}) AS _inner ORDER BY {order_by} LIMIT ${limit_param} OFFSET ${offset_param}"
    ))
}
