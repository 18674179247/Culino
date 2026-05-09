# Culino Frontend 架构重构方案

> 目标:把 `mobile/` 改名 `frontend/`,项目名 `culino-mobile` → `culino-frontend`。
> 源码从 `core/` + `feature/` 两级目录重新划分为 **feature / common / framework** 三层,
> 统一放入 `frontend/src/` 下。清理未使用模块。

---

## 1. 现状盘点

### 1.1 现有模块

| 现模块 | 状态 | 代码量 | 说明 |
|---|---|---|---|
| `core:common` | 使用中 | 3 个 .kt | `AppResult`、`Constants`、`Extensions` — 纯工具/常量 |
| `core:model` | 使用中 | 4 个 .kt | `Auth`、`User`、`ApiResponse`、`AI` — 通用 DTO |
| `core:network` | 使用中 | 12 个 .kt | **混合**:HttpClient/ApiClient/TokenProvider(基建) + ImageUploadApi/AiApiService(业务) |
| `core:data` | 使用中 | 3 个 .kt | `KeyValueStore`、`TokenStorage`、`PreferencesStorage` — 本地存储 |
| `core:ui` | 使用中 | 13 个 .kt | 组件 + 主题,跨 feature 共用 |
| `feature:user` | 使用中 | data/domain/presentation 齐全 | 登录、注册、个人资料 |
| `feature:recipe` | 使用中 | data/domain/presentation 齐全 | 菜谱 CRUD、搜索、冰箱搜菜 |
| `feature:social` | 使用中 | data/presentation | 收藏、烹饪记录 |
| `feature:ingredient` | 使用中 | data/presentation | 食材管理 |
| `feature:tool` | 使用中 | data/presentation | 购物清单、膳食计划 |
| **`feature:ai`** | **空壳** | **0 个 .kt** | `src/commonMain/kotlin/com/culino/feature/{ai,ingredient,tool}/` 下目录全空 — 删除 |

### 1.2 依赖现状(模块级)

```
app ──► core:{common,model,network,data,ui}  +  feature:{user,recipe,social,tool,ingredient}
feature:user ──► core:{common,model,network,data,ui} + feature:{social,recipe}
feature:recipe ──► core:{common,model,network,ui} + feature:{social,ingredient}
feature:social ──► core:{common,network,ui}
feature:tool ──► core:{common,network,ui}
feature:ingredient ──► core:{common,network,ui}
```

注意:feature 之间已有耦合(user 依赖 social+recipe,recipe 依赖 social+ingredient),
重构时这些交叉依赖按**同层允许**处理,不强制打平。

### 1.3 混合职责识别(需拆分)

`core:network` 里的:
- **基建类**:`HttpClient/Factory`、`HttpEngine` 的 expect/actual、`ApiClient`、`TokenProvider` interface、`AuthExpiredBus`、`BackendResponse`、`ApiResponse`、`parseUserIdFromToken`
- **通用业务 API**:`ImageUploadApi`(图片上传)、`AiApiService`(AI 服务,被 `feature:recipe` 直接使用)、`UploadResponse`

拆分:前者留 `framework:network`,后者去 `common:api`。

---

## 2. 目标架构

### 2.1 目录布局

```
frontend/                          # 原 mobile/
├── settings.gradle.kts            # rootProject.name = "culino-frontend"
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── gradlew / gradlew.bat
├── README.md
├── docs/                          # 保留
├── app/                           # KMP 壳(Android + iOS + Web 入口)
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── commonMain/            # App.kt、Navigation.kt、AppComponent(DI)
│   │   ├── androidMain/           # MainActivity、CulinoApplication、ImagePicker.android
│   │   ├── iosMain/               # MainViewController、ImagePicker.ios
│   │   └── wasmJsMain/            # Main.kt、index.html、ImagePicker.wasmJs
│   └── iosApp/                    # Xcode 工程
└── src/                           # 所有业务/通用/框架源码
    ├── framework/
    │   ├── network/               # Ktor 基建 + Token 抽象
    │   ├── storage/               # KeyValueStore(localStorage / DataStore)
    │   └── logging/               # Napier 薄封装(可选,见 §5)
    ├── common/
    │   ├── util/                  # 原 core:common(AppResult、Constants、Extensions)
    │   ├── model/                 # 原 core:model(Auth、User、AI DTO 等)
    │   ├── api/                   # ImageUploadApi、AiApiService(跨 feature 的业务 API)
    │   └── ui/                    # 原 core:ui(组件 + 主题)
    └── feature/
        ├── user/
        ├── recipe/
        ├── social/
        ├── ingredient/
        └── tool/
```

### 2.2 Gradle 模块命名

物理目录与模块路径对齐:

| Gradle path | 物理路径 |
|---|---|
| `:app` | `frontend/app` |
| `:framework:network` | `frontend/src/framework/network` |
| `:framework:storage` | `frontend/src/framework/storage` |
| `:framework:logging` | `frontend/src/framework/logging`(见 §5) |
| `:common:util` | `frontend/src/common/util` |
| `:common:model` | `frontend/src/common/model` |
| `:common:api` | `frontend/src/common/api` |
| `:common:ui` | `frontend/src/common/ui` |
| `:feature:user` | `frontend/src/feature/user` |
| `:feature:recipe` | `frontend/src/feature/recipe` |
| `:feature:social` | `frontend/src/feature/social` |
| `:feature:ingredient` | `frontend/src/feature/ingredient` |
| `:feature:tool` | `frontend/src/feature/tool` |

`settings.gradle.kts` 里对每个模块用 `project(":xxx").projectDir = file("src/xxx")` 显式指定物理路径。

### 2.3 依赖规则(强约束)

```
feature ──► common ──► framework
feature ──► framework   (允许跳跃,如 feature 直接用网络基建)
```

- **framework 永远不能依赖 common 或 feature**
- **common 永远不能依赖 feature**
- **feature 之间允许同层依赖**(保持现状:user↔social, user↔recipe, recipe↔social, recipe↔ingredient)

### 2.4 包名与代码位置

Kotlin 包名统一前缀 `com.culino`,按层级:

| 层 | 包名 | 物理源集 |
|---|---|---|
| framework:network | `com.culino.framework.network` | `commonMain/kotlin/com/culino/framework/network/` |
| framework:storage | `com.culino.framework.storage` | `commonMain/kotlin/com/culino/framework/storage/` |
| common:util | `com.culino.common.util` | — |
| common:model | `com.culino.common.model` | — |
| common:api | `com.culino.common.api` | — |
| common:ui | `com.culino.common.ui.{component,theme}` | — |
| feature:user | `com.culino.feature.user.{data,domain,presentation}` | — |

---

## 3. 文件级迁移映射

### 3.1 framework 层

**`framework:network`**(原 `core:network` 的基建部分)
```
HttpEngineFactory.kt (common + android/ios/jvm/wasmJs actual)
HttpClientFactory.kt
ApiClient.kt
TokenProvider.kt (含 AuthExpiredBus、parseUserIdFromToken)
BackendResponse.kt
ApiResponse.kt          # 注意:core/model 里也有同名 ApiResponse.kt — 保留 model 的那份,删 network 的
ApiClientTest.kt
```
依赖:Ktor、kotlinx-serialization-json、kotlinx-coroutines、Napier

**`framework:storage`**(原 `core:data`)
```
KeyValueStore.kt                    # 接口 + expect
KeyValueStore.nonWasm.kt           # DataStore 实现
KeyValueStore.wasmJs.kt            # localStorage 实现
TokenStorage.kt                     # 实现 TokenProvider(从 framework:network)
PreferencesStorage.kt
TokenStorageTest.kt
```
依赖:framework:network(为实现 TokenProvider)、datastore(nonWasm)、kotlinx-browser(wasmJs)

> **决策点 A**:`TokenStorage` 实现 `framework:network` 的 `TokenProvider` 接口 —
> 按依赖规则 framework 之间可以互相依赖(storage → network),但更干净的做法是把 `TokenProvider` 接口放 `framework:network`,
> `TokenStorage` 放 `framework:storage`,两者都是 framework,方向一致。**采用此方案**。

### 3.2 common 层

**`common:util`**(原 `core:common`)
```
AppResult.kt  Extensions.kt  Constants.kt  AppResultTest.kt
```
依赖:kotlinx-coroutines

**`common:model`**(原 `core:model`)
```
Auth.kt  User.kt  ApiResponse.kt  AI.kt  ApiResponseTest.kt
```
依赖:kotlinx-serialization-json、kotlinx-datetime

**`common:api`**(从 `core:network` 抽出)
```
AiApiService.kt
ImageUploadApi.kt
UploadResponse.kt
```
依赖:framework:network、common:model、Ktor client

**`common:ui`**(原 `core:ui`)
```
component/*.kt  (10 个组件)
theme/*.kt      (Color、Theme、Type)
```
依赖:common:model(组件里用了 User/Recipe 模型?需验证)、Compose、Coil

### 3.3 feature 层

各 feature 模块原样搬迁,只改依赖引用:

| feature | 新依赖 |
|---|---|
| user | common:{util,model,ui,api}、framework:{network,storage}、feature:{social,recipe} |
| recipe | common:{util,model,ui,api}、framework:network、feature:{social,ingredient} |
| social | common:{util,model,ui}、framework:network |
| ingredient | common:{util,model,ui}、framework:network |
| tool | common:{util,model,ui}、framework:network |

> `feature:user` 里 `ProfileViewModel` 用 `ImageUploadApi`,所以依赖 `common:api`。
> `feature:recipe` 里 `RecipeCreateViewModel` 用 `ImageUploadApi + AiApiService`,同样依赖 `common:api`。

### 3.4 app 模块

保持路径 `frontend/app/`,更新 `build.gradle.kts` 所有 `project(":core:xxx")` / `project(":feature:xxx")` 为新路径。

---

## 4. 需删除的内容

1. **`feature:ai` 整个模块**:源码为空,仅有 build 产物残留
2. **`core/network/ApiResponse.kt`** 与 `core/model/ApiResponse.kt` 重名(需 diff 确认,保留一份)
3. **所有旧的 `core/` 和 `feature/` 根目录**:迁移完成后删除(git mv 自动处理)
4. **build 目录**:迁移前先 `./gradlew clean` 避免残留
5. **Bazel 文件**(可选):`BUILD.bazel`、`WORKSPACE.bazel`、`.bazelrc` — 目前没在用(README 里没提),如果确认不用可以顺手清掉。**待确认**

---

## 5. 决策点

### 决策点 B:`framework:logging` 是否拆独立模块

现状:Napier 在 `core:network`、`feature:user`、`feature:recipe` 的 `build.gradle` 里分别 `implementation(libs.napier)`,
代码直接 `import io.github.aakira.napier.Napier`,**没有薄封装**。

两种选择:
- **B1(推荐)**:不单独建 `framework:logging`,让各模块直接依赖 `libs.napier`(现状);把日志算作 framework 的通用依赖,写入文档即可
- **B2**:建 `framework:logging` 做薄封装(`CulinoLogger.d/e/w`),统一 tag 规范和未来替换日志库的成本。但现在代码量极小,属于 YAGNI

**建议 B1**。§2.1 结构里的 `framework/logging` 先不建,等真的有抽象需求再加。

### 决策点 C:iosApp 是否也算 frontend

iosApp 是 Xcode 壳,保持 `frontend/app/iosApp/` 结构(上次已搬)。**无争议**。

### 决策点 D:HarmonyOS 后续扩展

根 docs 里有 HarmonyOS 集成规划。新架构下 HarmonyOS 入口自然落在 `frontend/app/src/harmonyMain/`,
feature/common/framework 三层无需变动,只需各模块加 `harmonyMain` 源集 + harmony actual。架构兼容。

---

## 6. 实施步骤(执行时按顺序)

### Step 1:准备
- `./gradlew clean`(清 build 产物)
- 提交当前状态

### Step 2:物理迁移(批量 git mv)
1. `git mv mobile frontend`
2. `rm -rf frontend/feature/ai`(空模块)
3. 创建新目录:`frontend/src/{framework,common,feature}`
4. 按映射 git mv 每个旧模块到新路径
   - `frontend/core/common` → `frontend/src/common/util`
   - `frontend/core/model` → `frontend/src/common/model`
   - `frontend/core/ui` → `frontend/src/common/ui`
   - `frontend/core/data` → `frontend/src/framework/storage`
   - `frontend/core/network` → `frontend/src/framework/network`
   - `frontend/feature/user` → `frontend/src/feature/user`
   - 其余 feature 同理
5. 把 `core:network` 里的 `AiApiService.kt`、`ImageUploadApi.kt`、`UploadResponse.kt` git mv 到新建的 `frontend/src/common/api/`
6. 删除 `frontend/core`、`frontend/feature` 空目录

### Step 3:改包名与 import
7. 全局替换:
   - `com.culino.core.common` → `com.culino.common.util`
   - `com.culino.core.model` → `com.culino.common.model`
   - `com.culino.core.ui` → `com.culino.common.ui`
   - `com.culino.core.data` → `com.culino.framework.storage`
   - `com.culino.core.network` → `com.culino.framework.network`(基建类)
   - `com.culino.core.network.AiApiService` → `com.culino.common.api.AiApiService`
   - `com.culino.core.network.ImageUploadApi` → `com.culino.common.api.ImageUploadApi`
   - `com.culino.core.network.UploadResponse` → `com.culino.common.api.UploadResponse`
8. 每个 .kt 文件顶部的 `package` 同步修改
9. 物理目录里的包路径同步(`com/culino/core/xxx` → `com/culino/common/xxx` 等)

### Step 4:Gradle 配置
10. `settings.gradle.kts`:
    - `rootProject.name = "culino-frontend"`
    - 删除旧 `include(":core:xxx")` `include(":feature:xxx")` `include(":composeApp")`
    - 新增 13 条 `include(":framework:network")` 等,每个加 `project(":xxx").projectDir = file("src/xxx")`
    - `include(":app")` 保持(app 目录独立于 src/)
11. 每个模块的 `build.gradle.kts`:改 `project(":core:xxx")` / `projects.core.xxx` → 新路径
12. `:app` 的依赖列表全面更新
13. 删除无用:`Bazel` 文件(决策 A 如确认不用)

### Step 5:验证
14. `./gradlew :app:compileKotlinWasmJs`
15. `./gradlew :app:compileDebugKotlinAndroid`
16. `./gradlew :app:compileKotlinIosArm64`(或 iosSimulatorArm64)
17. 单元测试:`./gradlew test`

### Step 6:更新周边
18. 根 `README.md`:结构图与命令(`cd mobile` → `cd frontend`)
19. `frontend/README.md`
20. `.run/*.sh`:CWD 从 `mobile/` 到 `frontend/`
21. `frontend/app/iosApp/iosApp.xcodeproj/project.pbxproj`:`cd $SRCROOT/../..` 改为 `cd $SRCROOT/../../..`(因为 frontend 多了一层)?—— **不需要**:SRCROOT 是 `frontend/app/iosApp/`,向上三级回到 `culino/`(git 根),但 gradle 在 `frontend/` 下,所以 `cd $SRCROOT/../..` 是正确的(回到 `frontend/`)。只有把 mobile → frontend 改名无需改 pbxproj
22. 检查 `docs/harmony-integration-plan.md` 里的路径引用

### Step 7:提交
23. 原子提交:一个重构 commit。大量 git mv 会被 git 识别为 rename,diff 清晰。

---

## 7. 风险与回滚

| 风险 | 应对 |
|---|---|
| 大量 import 改动漏掉,编译失败 | 分步编译,每步验证;全局 grep 确认 |
| kotlin-inject KSP 生成代码对旧包名有依赖 | Step 1 clean + Step 5 重编 |
| Xcode 工程 framework 搜索路径对旧目录名有引用 | §Step 6.21 已分析,mobile→frontend 重命名不影响 |
| IDE 索引错乱 | 完成后 Invalidate Caches |

**回滚**:如果中途失败,`git reset --hard HEAD^`(单 commit)即可全部复原。

---

## 8. 预期收益

- 目录命名与职责对齐,`framework` / `common` / `feature` 三层一目了然
- `core:network` 的混合职责消除,网络基建与业务 API 解耦
- 空模块清理,模块数从 11 减到 11(feature:ai 删除 -1,common:api 新增 +1,但总数不变)
- 包名 `com.culino.{framework,common,feature}` 与目录完全对应,新人上手更快
- 为 HarmonyOS 扩展预留自然的位置(只加 `harmonyMain` 源集,架构不动)
