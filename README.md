# Culino — 智能菜谱管理平台

一个全栈菜谱管理应用，包含 Rust 后端服务和 Kotlin Multiplatform 前端，支持 AI 驱动的营养分析与个性化推荐。

## 项目结构

```
culino/
├── backend/       # Rust 后端服务（Axum + PostgreSQL）
└── frontend/      # KMP 前端（Compose Multiplatform，Android / iOS / Web）
```

## 功能特性

- **菜谱管理** — 创建、编辑、搜索、随机推荐，创建时自动触发 AI 营养分析
- **食材与调料** — 分类浏览、标签管理
- **社交互动** — 收藏菜谱、烹饪记录、点赞、评论
- **工具箱** — 购物清单（支持批量添加）、膳食计划
- **AI 智能** — 营养分析、个性化推荐、相似菜谱推荐、用户偏好画像、菜谱图片识别、购物清单文本解析（基于 DeepSeek API）
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

### 前端

| 组件 | 技术 |
|------|------|
| 框架 | Kotlin Multiplatform + Compose Multiplatform |
| 网络 | Ktor 3.1 |
| 序列化 | Kotlinx Serialization |
| 依赖注入 | kotlin-inject |
| 图片加载 | Coil 3 |
| 导航 | Navigation Compose |
| 本地存储 | DataStore / localStorage (Web) |

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

### 前端

```bash
cd frontend

# Android
./gradlew :app:assembleDebug

# iOS（需要 macOS + Xcode）
# 通过 Xcode 打开 app/iosApp/iosApp.xcodeproj 运行

# Web
./gradlew :app:wasmJsBrowserDevelopmentRun
```

## API 概览

所有接口以 `/api/v1` 为前缀。

| 模块 | 路径 | 说明 |
|------|------|------|
| 用户 | `/user` | 注册、登录、登出、个人信息 |
| 菜谱 | `/recipe` | CRUD、搜索、随机推荐 |
| 食材 | `/ingredient` | 食材、调料、标签管理 |
| 社交 | `/social` | 收藏、烹饪记录、点赞、评论 |
| 工具 | `/tool` | 购物清单、膳食计划 |
| 上传 | `/upload` | 图片上传与删除 |
| AI | `/ai` | 营养分析、智能推荐、偏好分析、菜谱识别、购物清单解析 |

完整 API 文档请启动服务后访问 Swagger UI。

## 前端架构

采用分层多模块架构,依赖方向:`feature → common → framework`：

```
frontend/
├── app/                   # 应用壳(Android / iOS / Web 入口)
│   └── iosApp/            # iOS Xcode 工程
└── src/
    ├── framework/         # 技术基建(不依赖业务)
    │   ├── network/       # Ktor HttpClient、ApiClient、TokenProvider
    │   └── storage/       # KeyValueStore(DataStore / localStorage)
    ├── common/            # 跨 feature 通用
    │   ├── util/          # AppResult、Constants、Extensions
    │   ├── model/         # 业务数据模型
    │   ├── api/           # 跨 feature 的业务 API(AI、图片上传)
    │   └── ui/            # 公共组件与 Material 3 主题
    └── feature/           # 业务功能(每个 feature 分 data/domain/presentation)
        ├── user/          # 用户
        ├── recipe/        # 菜谱
        ├── social/        # 社交
        ├── ingredient/    # 食材
        └── tool/          # 工具
```

每个 feature 模块分为 `data` → `domain` → `presentation` 三层。

## 环境要求

- Rust 1.86+
- Docker & Docker Compose
- JDK 17+
- Android Studio / Xcode（前端开发）

## 许可证

[MIT](LICENSE) © 云山苍苍
