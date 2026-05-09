# Culino Frontend

基于 Kotlin Multiplatform + Compose Multiplatform 的跨平台菜谱管理应用,支持 Android / iOS / Web。

## 功能

- **菜谱浏览** — 列表、搜索、随机推荐、详情查看（含营养信息、点赞数、评论数）
- **菜谱创建** — 编辑食材、调料、步骤，上传封面和图片
- **收藏管理** — 收藏 / 取消收藏菜谱
- **社交互动** — 烹饪记录、点赞、评论
- **购物清单** — 创建清单、管理商品条目、批量添加、AI 文本解析
- **膳食计划** — 按日期规划每餐菜谱
- **用户系统** — 注册、登录、个人资料、头像上传
- **AI 智能** — 营养分析、个性化推荐、偏好画像、菜谱图片识别、购物清单文本解析

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

依赖方向:`feature → common → framework`(framework 不依赖任何业务层)。

```
culino-frontend/
├── app/                          # 应用壳
│   ├── src/
│   │   ├── commonMain/           # App、Navigation、DI
│   │   ├── androidMain/          # MainActivity、Android ImagePicker
│   │   ├── iosMain/              # MainViewController、iOS ImagePicker
│   │   └── wasmJsMain/           # Main.kt(ComposeViewport)、index.html、Web ImagePicker
│   └── iosApp/                   # iOS Xcode 工程
└── src/
    ├── framework/                # 技术基建,可独立于业务复用
    │   ├── network/              # HttpClient、ApiClient、TokenProvider
    │   └── storage/              # KeyValueStore(DataStore / localStorage)
    ├── common/                   # 跨 feature 共享
    │   ├── util/                 # AppResult、Constants、Extensions
    │   ├── model/                # 业务数据模型(User、Auth、AI、ApiResponse)
    │   ├── api/                  # 跨 feature 的业务 API(AiApiService、ImageUploadApi)
    │   └── ui/                   # 公共组件 + Material 3 主题
    └── feature/                  # 业务功能
        ├── user/                 # 登录、注册、个人资料
        ├── recipe/               # 菜谱 CRUD、搜索、冰箱搜菜
        ├── social/               # 收藏、烹饪记录
        ├── ingredient/           # 食材与调料
        └── tool/                 # 购物清单、膳食计划
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
- 后端服务运行中（见 [backend](../backend)）

### Android

```bash
# 构建 Debug APK
./gradlew :app:assembleDebug

# 构建并安装到设备（需要先启动后端服务）
./run-debug.sh
```

`run-debug.sh` 会自动设置 ADB 端口转发，将设备的 3000 和 9000 端口映射到本机，然后构建安装并启动应用。

### iOS

通过 Xcode 打开 iOS 项目，选择目标设备后运行。

### 配置

Debug 模式下 API 地址默认为 `http://127.0.0.1:3000/api/v1/`,可在 `app/build.gradle.kts` 的 `buildConfigField` 中修改。

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

本应用配合 [backend](../backend) 使用，后端提供以下 API：

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
