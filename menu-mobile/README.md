# Menu Mobile

基于 Kotlin Multiplatform + Compose Multiplatform 的跨平台菜谱管理应用，支持 Android 和 iOS。

## 功能

- **菜谱浏览** — 列表、搜索、随机推荐、详情查看
- **菜谱创建** — 编辑食材、调料、步骤，上传封面和图片
- **收藏管理** — 收藏 / 取消收藏菜谱
- **烹饪记录** — 记录烹饪历史、评分、备注
- **购物清单** — 创建清单、管理商品条目
- **膳食计划** — 按日期规划每餐菜谱
- **用户系统** — 注册、登录、个人资料、头像上传
- **AI 智能** — 营养分析、个性化推荐、偏好画像

## 截图

<!-- TODO: 添加应用截图 -->

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin 2.1 |
| UI 框架 | Compose Multiplatform 1.7 |
| 网络 | Ktor 3.1 |
| 序列化 | Kotlinx Serialization |
| 依赖注入 | kotlin-inject |
| 图片加载 | Coil 3 |
| 导航 | Navigation Compose |
| 本地存储 | DataStore Preferences |
| 日志 | Napier |

## 项目结构

```
menu-mobile/
├── composeApp/               # 应用入口
│   └── src/
│       ├── commonMain/       # 跨平台代码（App、Navigation、DI）
│       ├── androidMain/      # Android 入口（MainActivity）
│       └── iosMain/          # iOS 入口（MainViewController）
├── core/
│   ├── common/               # 通用工具、扩展函数、Result 封装
│   ├── model/                # 跨模块数据模型（User、Auth、AI）
│   ├── network/              # 网络层（HttpClient、ApiClient、TokenProvider）
│   ├── data/                 # 数据持久化（DataStore、TokenStorage）
│   └── ui/                   # 公共 UI 组件与 Material 3 主题
├── feature/
│   ├── user/                 # 用户模块（登录、注册、个人资料）
│   ├── recipe/               # 菜谱模块（列表、详情、创建）
│   ├── social/               # 社交模块（收藏、烹饪记录）
│   ├── ingredient/           # 食材模块
│   ├── tool/                 # 工具模块（购物清单、膳食计划）
│   └── ai/                   # AI 模块（营养分析、推荐）
└── docs/                     # 开发文档
```

### 模块架构

每个 feature 模块采用 Clean Architecture 分层：

```
feature/{module}/
├── data/                     # 数据层
│   ├── *Models.kt            # 网络数据模型
│   ├── *Api.kt               # API 接口
│   └── *Repository.kt        # 数据仓库
├── domain/                   # 业务逻辑层
│   └── *UseCases.kt          # 用例
└── presentation/             # 表现层
    ├── *Screen.kt            # Composable UI
    ├── *ViewModel.kt         # 视图模型
    └── *State.kt             # UI 状态
```

## 快速开始

### 环境要求

- JDK 17+
- Android Studio Ladybug 或更高版本
- Xcode 15+（iOS 开发）
- 后端服务运行中（见 [menu-backend](../menu-backend)）

### Android

```bash
# 构建 Debug APK
./gradlew :composeApp:assembleDebug

# 构建并安装到设备（需要先启动后端服务）
./run-debug.sh
```

`run-debug.sh` 会自动设置 ADB 端口转发，将设备的 3000 和 9000 端口映射到本机，然后构建安装并启动应用。

### iOS

通过 Xcode 打开 iOS 项目，选择目标设备后运行。

### 配置

Debug 模式下 API 地址默认为 `http://127.0.0.1:3000/api/v1/`，可在 `composeApp/build.gradle.kts` 的 `buildConfigField` 中修改。

## 导航结构

```
Login ─── Register
  │
  └── Main（底部导航栏）
        ├── 首页（菜谱列表）
        ├── 菜谱（我的菜谱）
        ├── + 创建（扇形快捷菜单）
        │     ├── 创建菜谱
        │     ├── 记录烹饪
        │     ├── 购物清单
        │     └── 膳食计划
        ├── 收藏
        └── 我的（个人资料）
              ├── 烹饪记录
              ├── 购物清单 → 清单详情
              └── 膳食计划
```

## 对应后端接口

本应用配合 [menu-backend](../menu-backend) 使用，后端提供以下 API：

| 模块 | 接口前缀 |
|------|---------|
| 用户 | `/api/v1/user` |
| 菜谱 | `/api/v1/recipe` |
| 食材 | `/api/v1/ingredient` |
| 社交 | `/api/v1/social` |
| 工具 | `/api/v1/tool` |
| 上传 | `/api/v1/upload` |
| AI | `/api/v1/ai` |

## 许可证

[MIT](../LICENSE) © 云山苍苍
