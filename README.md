# Culino — 智能菜谱管理平台

一个全栈菜谱管理应用，包含 Rust 后端服务和 Kotlin Multiplatform 移动端，支持 AI 驱动的营养分析与个性化推荐。

## 项目结构

```
culino/
├── backend/      # Rust 后端服务（Axum + PostgreSQL）
└── mobile/       # KMP 移动端（Compose Multiplatform，Android / iOS）
```

## 功能特性

- **菜谱管理** — 创建、编辑、搜索、随机推荐
- **食材与调料** — 分类浏览、标签管理
- **社交互动** — 收藏菜谱、烹饪记录、评分
- **工具箱** — 购物清单、膳食计划
- **AI 智能** — 营养分析、个性化推荐、相似菜谱推荐、用户偏好画像（基于 DeepSeek API）
- **图片上传** — S3 兼容对象存储（RustFS / MinIO）

## 技术栈

### 后端

| 组件 | 技术 |
|------|------|
| Web 框架 | Axum 0.8 |
| 数据库 | PostgreSQL 16 + SQLx 0.8 |
| 缓存 | Redis 7 |
| 认证 | JWT + Argon2 |
| 对象存储 | rust-s3（RustFS / MinIO） |
| AI | DeepSeek API |
| API 文档 | Utoipa + Swagger UI |
| 容器化 | Docker + Docker Compose |

### 移动端

| 组件 | 技术 |
|------|------|
| 框架 | Kotlin Multiplatform + Compose Multiplatform |
| 网络 | Ktor 3.1 |
| 序列化 | Kotlinx Serialization |
| 依赖注入 | kotlin-inject |
| 图片加载 | Coil 3 |
| 导航 | Navigation Compose |
| 本地存储 | DataStore |

## 快速开始

### 后端

```bash
cd backend

# 启动依赖服务（PostgreSQL、Redis、RustFS）
docker compose up -d db redis rustfs

# 配置环境变量
cp .env.example .env
# 编辑 .env 填入实际配置

# 运行服务
cargo run

# 访问 API 文档
open http://localhost:3000/swagger-ui/
```

### 移动端

```bash
cd mobile

# Android
./gradlew :composeApp:assembleDebug

# iOS（需要 macOS + Xcode）
# 通过 Xcode 打开 iOS 项目运行
```

## API 概览

所有接口以 `/api/v1` 为前缀。

| 模块 | 路径 | 说明 |
|------|------|------|
| 用户 | `/user` | 注册、登录、登出、个人信息 |
| 菜谱 | `/recipe` | CRUD、搜索、随机推荐 |
| 食材 | `/ingredient` | 食材、调料、标签管理 |
| 社交 | `/social` | 收藏、烹饪记录 |
| 工具 | `/tool` | 购物清单、膳食计划 |
| 上传 | `/upload` | 图片上传与删除 |
| AI | `/ai` | 营养分析、智能推荐、偏好分析 |

完整 API 文档请启动服务后访问 Swagger UI。

## 移动端架构

采用多模块 Clean Architecture：

```
mobile/
├── core/
│   ├── common/       # 通用工具与扩展
│   ├── model/        # 数据模型
│   ├── network/      # 网络层（Ktor）
│   ├── data/         # 数据持久化
│   └── ui/           # 公共 UI 组件与主题
├── feature/
│   ├── user/         # 用户模块
│   ├── recipe/       # 菜谱模块
│   ├── social/       # 社交模块
│   ├── ingredient/   # 食材模块
│   ├── tool/         # 工具模块
│   └── ai/           # AI 模块
└── composeApp/       # 应用入口与导航
```

每个 feature 模块分为 `data` → `domain` → `presentation` 三层。

## 环境要求

- Rust 1.86+
- Docker & Docker Compose
- JDK 17+
- Android Studio / Xcode（移动端开发）

## 许可证

[MIT](LICENSE) © 云山苍苍
