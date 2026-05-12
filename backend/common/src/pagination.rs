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
    /// 最大允许页码。避免攻击者通过 `page=1_000_000` 触发深度 OFFSET 扫描。
    /// 10_000 * 100(最大 page_size) = 1_000_000 条上限,足够常规业务。
    pub const MAX_PAGE: i64 = 10_000;

    /// 计算 SQL OFFSET 值
    pub fn offset(&self) -> i64 {
        (self.page() - 1) * self.limit()
    }

    /// 获取每页条数（即 SQL LIMIT），范围 [1, 100]
    pub fn limit(&self) -> i64 {
        self.page_size.unwrap_or(20).clamp(1, 100)
    }

    /// 获取当前页码，范围 [1, MAX_PAGE]
    pub fn page(&self) -> i64 {
        self.page.unwrap_or(1).clamp(1, Self::MAX_PAGE)
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

/// 分页 ORDER BY 白名单
///
/// 避免向 `paginate_sql` 传入任意字符串。所有可用排序键都必须在此枚举中显式列出,
/// 从而彻底杜绝 SQL 注入面。
#[derive(Debug, Clone, Copy)]
pub enum OrderBy {
    /// `_inner.created_at DESC` — 创建时间倒序(最新优先)
    CreatedAtDesc,
    /// `_inner.created_at ASC` — 创建时间正序
    CreatedAtAsc,
    /// `_inner.id DESC` — 主键倒序
    IdDesc,
}

impl OrderBy {
    pub fn as_sql(self) -> &'static str {
        match self {
            OrderBy::CreatedAtDesc => "_inner.created_at DESC",
            OrderBy::CreatedAtAsc => "_inner.created_at ASC",
            OrderBy::IdDesc => "_inner.id DESC",
        }
    }
}

/// 将基础 SQL 包装为带 `COUNT(*) OVER() AS total_count` 的单次分页查询
///
/// `order_by` 使用白名单枚举,调用方无法传入用户输入,从根源杜绝注入。
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
    order_by: OrderBy,
    limit_param: u32,
    offset_param: u32,
) -> String {
    format!(
        "SELECT *, COUNT(*) OVER() AS total_count FROM ({base_sql}) AS _inner ORDER BY {} LIMIT ${limit_param} OFFSET ${offset_param}",
        order_by.as_sql()
    )
}
