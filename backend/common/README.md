# culino-common

公共基础设施 crate，被所有业务模块依赖。

## 模块

| 模块 | 说明 |
|------|------|
| `auth` | JWT 认证（签发/验证）、密码哈希（Argon2）、`AuthUser` 请求提取器 |
| `config` | `AppConfig` 应用配置，从环境变量加载 |
| `state` | `AppState` 全局状态（数据库连接池 + Redis + S3 + 配置） |
| `error` | `AppError` 统一错误类型，自动映射 HTTP 状态码 |
| `pagination` | `PaginationParams` 分页参数 + `PaginatedResponse<T>` 分页响应 |
| `response` | `ApiResponse<T>` 统一响应格式 |
| `redis` | Redis 连接创建与 Token 管理 |
| `s3` | S3 兼容对象存储客户端封装（上传、删除、URL 生成） |
| `tx` | 数据库事务辅助 |

## 错误类型

| 变体 | HTTP 状态码 |
|------|------------|
| `NotFound` | 404 |
| `Unauthorized` | 401 |
| `Forbidden` | 403 |
| `BadRequest` | 400 |
| `Validation` | 422 |
| `Conflict` | 409 |
| `Internal` | 500 |
