# Culino Backend

基于 Rust 的智能菜谱管理后端服务，提供菜谱、食材、用户、社交、工具等功能的 REST API。

## 技术栈

- Axum 0.8 — Web 框架
- SQLx 0.8 — 类型安全的异步 PostgreSQL 驱动
- Tokio — 异步运行时
- Redis 7 — 缓存与 Token 管理
- JWT + Argon2 — 用户认证
- DeepSeek API — AI 营养分析与智能推荐
- rust-s3 — S3 兼容对象存储（MinIO / RustFS）
- Utoipa + Swagger UI — API 文档

## 项目结构

```
src/                        # 应用入口
├── main.rs                 # 启动流程
├── openapi.rs              # OpenAPI 文档构建
└── router.rs               # 路由构建

common/                     # 公共基础设施（认证、配置、错误处理、分页、行为日志 trait）

features/
├── user/                   # 用户模块（注册、登录、个人信息）
├── recipe/                 # 菜谱模块（CRUD、搜索、随机推荐）
├── ingredient/             # 食材模块（食材、调料、标签管理）
├── social/                 # 社交模块（收藏、烹饪记录）
├── tool/                   # 工具模块（购物清单、膳食计划）
├── upload/                 # 上传模块（图片上传到 S3 兼容存储）
└── ai/                     # AI 模块（营养分析、智能推荐、偏好分析）
```

## 快速开始

```bash
# 配置环境变量
cp .env.example .env
# 编辑 .env 填入数据库连接信息

# 启动服务
cargo run

# 访问 Swagger UI
open http://localhost:3000/swagger-ui/
```

## API 概览

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| 用户 | `/api/v1/user` | 注册、登录、登出、个人信息 |
| 菜谱 | `/api/v1/recipe` | 菜谱 CRUD、搜索、随机推荐 |
| 食材 | `/api/v1/ingredient` | 食材、调料、标签管理 |
| 社交 | `/api/v1/social` | 收藏、烹饪记录 |
| 工具 | `/api/v1/tool` | 购物清单、膳食计划 |
| 上传 | `/api/v1/upload` | 图片上传与删除（S3 兼容存储） |
| AI | `/api/v1/ai` | 营养分析、智能推荐、偏好分析、行为日志 |

## 文档

- [功能文档](./docs/功能文档.md)
- [架构规范](./docs/架构设计.md)
- [AI 集成方案](./docs/AI集成方案.md)
- [AI 完成总结](./docs/AI功能完成.md)

## 许可证

[MIT](../LICENSE) © 云山苍苍
