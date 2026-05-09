# Culino Mobile — KMP 多平台移动端技术设计

## 概述

基于 backend REST API 构建对应的移动端应用，使用 Kotlin Multiplatform (KMP) + Compose Multiplatform 实现 Android、iOS、HarmonyOS NEXT 三端统一开发。覆盖后端全部 API 功能，包含消费者端和管理端。

## 目标平台

- Android (minSdk 26+)
- iOS (16+)
- HarmonyOS NEXT (5.0+)，通过 KMP 鸿蒙适配方案支持

## 技术栈

| 层面 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Kotlin | 2.1+ | KMP 多平台 |
| UI | Compose Multiplatform | 1.7+ | 三端共享声明式 UI |
| 构建 | Bazel + Gradle | — | Bazel 构建，Gradle IDE 索引 |
| 网络 | Ktor Client | 3.x | CIO(Android/Harmony)、Darwin(iOS) |
| 序列化 | kotlinx.serialization | — | JSON，与 Ktor 集成 |
| 本地数据库 | SQLDelight | 2.x | 多平台类型安全 SQL |
| KV 存储 | DataStore Multiplatform | — | Token、偏好设置 |
| DI | kotlin-inject | 0.7+ | 编译时依赖注入，KSP |
| 导航 | Compose Navigation | — | 类型安全多平台路由 |
| 图片加载 | Coil | 3.x | 多平台图片加载与缓存 |
| 异步 | kotlinx.coroutines | — | Flow、StateFlow |
| 日期 | kotlinx-datetime | — | 多平台日期处理 |
| 日志 | Napier | — | KMP 日志 |
| 测试 | kotlin.test + Turbine | — | 单元测试 + Flow 测试 |

## 构建系统

双构建系统策略：

- **Gradle**：负责 IDE 索引、依赖解析、版本目录管理（`gradle/libs.versions.toml`）。开发者在 IDE 中获得完整的代码补全、跳转、重构支持。
- **Bazel**：负责实际编译和产物构建。使用 `rules_kotlin`、`rules_jvm_external`、`rules_apple` 等规则集。增量构建和远程缓存能力在多 module 项目中优势显著。

Bazel 关键配置：
- `WORKSPACE.bazel`：工作区定义，外部依赖声明
- 每个 module 下 `BUILD.bazel`：模块构建规则
- `rules_kotlin`：Kotlin/KMP 编译
- `rules_jvm_external`：Maven 依赖管理（与 Gradle version catalog 保持同步）

## 项目结构

```
culino-mobile/
├── BUILD.bazel
├── WORKSPACE.bazel
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml
│
├── core/
│   ├── network/          # Ktor Client、API 拦截器、Token 管理、请求/响应模型
│   ├── data/             # SQLDelight 数据库、DataStore、缓存策略
│   ├── model/            # 共享数据模型（DTO、Domain Entity、映射器）
│   ├── ui/               # Design System、主题、通用 Compose 组件
│   └── common/           # 工具类、扩展函数、常量、Result 封装
│
├── feature/
│   ├── user/             # 注册、登录、个人资料、退出
│   ├── recipe/           # 菜谱 CRUD、搜索、随机推荐
│   ├── ingredient/       # 食材、调料、标签浏览与管理
│   ├── social/           # 收藏、烹饪日志
│   ├── tool/             # 购物清单、膳食计划
│   └── upload/           # 图片选择、压缩、上传（共享组件）
│
├── composeApp/
│   ├── src/commonMain/   # App 入口、导航图、主题配置
│   ├── src/androidMain/  # Android Application、Activity
│   ├── src/iosMain/      # iOS 入口适配
│   └── src/harmonyMain/  # HarmonyOS NEXT 入口适配
│
└── platform/
    ├── src/commonMain/   # expect 声明（图片选择、文件、权限、设备信息）
    ├── src/androidMain/  # Android actual 实现
    ├── src/iosMain/      # iOS actual 实现
    └── src/harmonyMain/  # HarmonyOS actual 实现
```

每个 feature module 内部结构：
```
feature/xxx/
├── src/commonMain/kotlin/
│   ├── data/             # Repository 实现、API Service 定义
│   ├── domain/           # UseCase、Domain Model、Repository 接口
│   └── presentation/     # ViewModel (MVI)、UiState、Screen Composable
└── BUILD.bazel
```

## 分层架构

采用 Clean Architecture + MVI 单向数据流：

```
┌──────────────────────────────────────────┐
│              Presentation                │
│  Screen (Compose) ← ViewModel ← UiState │
│       └── Intent ──→ ViewModel           │
├──────────────────────────────────────────┤
│                Domain                    │
│     UseCase ← Repository (interface)     │
├──────────────────────────────────────────┤
│                 Data                     │
│  RepositoryImpl ← RemoteDataSource(Ktor) │
│                 ← LocalDataSource(SQL)   │
└──────────────────────────────────────────┘
```

依赖方向：Presentation → Domain ← Data。Domain 层不依赖任何框架。

### MVI 模式

每个页面由三部分组成：

- **UiState**：不可变数据类，描述页面完整状态
- **Intent**：密封接口，描述用户意图/事件
- **ViewModel**：接收 Intent，调用 UseCase，更新 StateFlow<UiState>

示例（菜谱搜索）：

```kotlin
data class RecipeSearchState(
    val query: String = "",
    val recipes: List<RecipeItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface RecipeSearchIntent {
    data class UpdateQuery(val query: String) : RecipeSearchIntent
    data object Search : RecipeSearchIntent
    data object LoadRandom : RecipeSearchIntent
}

class RecipeSearchViewModel(
    private val searchRecipes: SearchRecipesUseCase,
    private val getRandomRecipes: GetRandomRecipesUseCase
) {
    private val _state = MutableStateFlow(RecipeSearchState())
    val state: StateFlow<RecipeSearchState> = _state.asStateFlow()

    fun onIntent(intent: RecipeSearchIntent) { ... }
}
```

### 缓存策略

- 网络优先：请求成功后写入 SQLDelight 本地库
- 无网络回退：读取本地缓存数据
- TTL 机制：缓存过期后下次联网自动刷新
- 优先缓存：收藏列表、菜谱详情、食材/调料基础数据

## 网络层设计

### Ktor Client 配置

```kotlin
val httpClient = HttpClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Auth) {
        bearer {
            loadTokens { BearerTokens(tokenStore.getAccessToken(), "") }
            refreshTokens { null } // JWT 过期后重新登录
        }
    }
    install(Logging) { level = LogLevel.BODY }
    defaultRequest { url("https://your-api.com/api/v1/") }
}
```

### API 响应模型

与后端 `ApiResponse<T>` 对齐：

```kotlin
@Serializable
data class ApiResponse<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: String? = null
)
```

### 引擎选择

- Android / HarmonyOS：CIO 引擎
- iOS：Darwin 引擎

## 平台适配层

通过 `expect/actual` 机制处理平台差异：

```kotlin
// commonMain
expect class ImagePicker { suspend fun pickImage(): ByteArray? }
expect class FileProvider { fun getCacheDir(): String }
expect fun getDeviceInfo(): DeviceInfo
```

各平台提供 actual 实现：
- Android：ActivityResult API、Context
- iOS：PHPickerViewController、FileManager
- HarmonyOS：对应鸿蒙 API

## 功能模块与页面规划

### feature-user（用户模块）

| 页面 | 对应 API | 说明 |
|------|----------|------|
| 登录页 | POST /user/login | 用户名/密码登录 |
| 注册页 | POST /user/register | 创建账号 |
| 个人资料页 | GET/PUT /user/me | 查看/编辑昵称、头像 |
| 设置页 | POST /user/logout | 退出登录、缓存管理 |

### feature-recipe（菜谱模块）

| 页面 | 对应 API | 说明 |
|------|----------|------|
| 首页/发现页 | GET /recipe/random | 随机推荐、分类入口 |
| 搜索页 | GET /recipe/search | 关键词、难度、时间、标签、食材多条件筛选 |
| 菜谱详情页 | GET /recipe/{id} | 封面、食材、调料、步骤（带图）、收藏按钮 |
| 创建/编辑页 | POST/PUT /recipe | 表单：标题、描述、难度、时间、份量、食材、调料、步骤编辑 |

### feature-ingredient（食材管理模块）

| 页面 | 对应 API | 说明 |
|------|----------|------|
| 食材列表页 | GET /ingredient/ingredients | 按分类浏览 |
| 调料列表页 | GET /ingredient/seasonings | 全部调料 |
| 标签列表页 | GET /ingredient/tags | 按类型分组（菜系/口味/场景/饮食） |
| 管理页面 | POST/PUT/DELETE 各管理接口 | admin 角色：食材/调料/标签增删改 |

### feature-social（社交模块）

| 页面 | 对应 API | 说明 |
|------|----------|------|
| 收藏列表页 | GET /social/favorites | 我的收藏菜谱 |
| 烹饪日志列表页 | GET /social/cooking-logs | 历史烹饪记录 |
| 日志创建/编辑页 | POST/PUT /social/cooking-logs | 选菜谱、评分、笔记、日期 |

### feature-tool（工具模块）

| 页面 | 对应 API | 说明 |
|------|----------|------|
| 购物清单列表页 | GET /tool/shopping-lists | 所有清单 |
| 购物清单详情页 | GET/POST/PUT/DELETE 清单项接口 | 清单项管理（勾选、增删改） |
| 膳食计划页 | GET/POST/PUT/DELETE /tool/meal-plans | 日历视图，按日期和餐次安排菜谱 |

### feature-upload（上传模块）

非独立页面，作为共享组件嵌入菜谱编辑、头像修改等场景：
- 图片选择 → 压缩 → POST /upload/image → 返回 URL
- 支持格式：JPEG、PNG、GIF、WebP
- 大小限制：15MB

## 导航结构

```
BottomNavigation
├── 首页（发现/推荐）
├── 搜索
├── 工具（购物清单 + 膳食计划）
└── 我的（收藏、日志、资料、管理入口）
```

admin 角色在「我的」页面显示额外的管理入口（食材/调料/标签管理）。

## 模块依赖关系

```
composeApp → feature-* → core/*
feature-* → core/model, core/network, core/ui, core/common
feature-* 之间无直接依赖
core/network → core/model, core/common
core/data → core/model, core/common
core/ui → core/model, core/common
platform → core/common
```

## 测试策略

- **Domain 层**：纯 Kotlin 单元测试，mock Repository 接口
- **ViewModel**：使用 Turbine 测试 StateFlow 状态变化
- **Repository**：mock Ktor Client 和 SQLDelight，验证缓存逻辑
- **UI**：Compose UI 测试（Android 端），截图测试
- **集成测试**：端到端 API 调用测试（连接真实后端）
