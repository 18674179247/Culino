# HarmonyOS 端对接方案（基于 ovCompose）

本文档记录 Culino 项目鸿蒙端（HarmonyOS NEXT）对接的技术方案与实施计划。

## 一、方案选型

对比了三种 HarmonyOS 原生应用对接路径：

| 维度 | ovCompose | KuiklyUI | ArkTS 原生 |
|---|---|---|---|
| 出品方 | 腾讯视频团队 | 腾讯 TDS | 华为官方 |
| 支持平台 | Android / iOS / 鸿蒙 | Android / iOS / 鸿蒙 / Web / 小程序 / macOS | 鸿蒙 |
| 底层架构 | Fork JetBrains CMP，魔改加鸿蒙 target | 自研渲染引擎 + Compose DSL | 纯鸿蒙原生 |
| 渲染方式 | Skia 自绘 | 平台原生控件 | ArkUI 原生 |
| Compose API 兼容 | 完全兼容（包名不变） | 兼容但包名改为 `com.tencent.kuikly.compose` | 不适用 |
| Kotlin 版本 | 定制版 `2.0.21-KBA-005` | 定制版 `2.0.21-KBA-010` / `2.1.21` | 不涉及 |
| 包体积 | 大（带 Skia） | 小（AOT Android ~300KB） | 最小 |
| 代码复用 | 高（Compose 代码几乎直接用） | 中-高（需改 import） | 0 |
| 生态/文档 | 少 | 齐全 | 华为官方最全 |

**选定方案：ovCompose**。理由：项目已经基于 Compose Multiplatform 构建，ovCompose 可以最大化复用现有 Kotlin + Compose 代码，不需要重写 UI。

## 二、架构总览

```
culino/
├── backend/                           Rust 后端（不变）
└── mobile/
    ├── core/                          共享业务逻辑（扩展 ohosArm64 target）
    │   ├── common
    │   ├── model
    │   ├── network
    │   ├── data
    │   └── ui
    ├── feature/                       业务模块（扩展 ohosArm64 target）
    │   ├── recipe
    │   ├── user
    │   └── ...
    ├── composeApp/                    Android + iOS 壳
    ├── composeHarmonyShared/          鸿蒙专用 Compose UI 入口（新增）
    └── harmonyApp/                    DevEco Studio 鸿蒙壳（新增）
        └── entry/src/main/
            ├── cpp/                   napi_init.cpp + libkn_api.h
            ├── ets/                   ArkTS 页面
            └── libs/                  compose.har + skikobridge.har + libkn.so
```

**核心思路**：`composeApp` 只产出 Android/iOS；新建 `composeHarmonyShared` 导出鸿蒙 `.so`。`harmonyApp` 是纯鸿蒙壳子，通过 NAPI 桥接调用 Compose。

## 三、关键版本约束

| 组件 | 当前版本 | ovCompose 要求 | 是否需降级 |
|---|---|---|---|
| Kotlin | 2.1.20 | `2.0.21-KBA-005` | 是 |
| Compose Multiplatform | 1.8.0 | `1.6.1-KBA-002` | 是 |
| kotlinx-coroutines | 1.9.x | `1.8.0-KBA-001` | 是 |
| AGP | 8.7.3 | 8.5.2 | 是 |
| DevEco Studio | - | 5.1.0+（API 18+） | 需安装 |
| HarmonyOS | - | NEXT 5.0.0 (API 12+) | 目标平台 |

**降级风险**：
- Navigation 3 在 1.6.1 还未支持，`Navigation.kt` 需要改回 Navigation 2.x 或自研路由
- Material 3 的部分新 API 可能缺失
- `SharedTransitionLayout` 在 1.6.1 稳定性需验证

## 四、KBA 版本 Maven 仓库

ovCompose 依赖腾讯自建的 Maven 仓库：

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven("https://mirrors.tencent.com/repository/maven/tencent_public")
        google()
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        maven("https://mirrors.tencent.com/repository/maven/tencent_public")
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
```

## 五、关键决策点

### 决策 1：全降级 vs 物理隔离

**方案 A - 全降级**
- 全项目统一用 KBA 版本（Kotlin 2.0.21-KBA / Compose 1.6.1-KBA）
- 优：代码复用最大化，一套代码跑三端
- 劣：Android/iOS 丢失 Compose 1.8 新特性（Navigation 3 等），需要全面回归测试

**方案 B - 物理隔离（推荐）**
- 现有 `composeApp/` 保持 Compose 1.8 不动，继续服务 Android/iOS
- 新建 `composeHarmonyShared/` 用 Compose 1.6.1-KBA，UI 代码从 `composeApp/` 复制
- 优：Android/iOS 零风险；鸿蒙端独立演进
- 劣：第一版鸿蒙 UI 有部分复制；后续需用 diff 工具同步两端 UI 变更
- 等 ovCompose 追上 Compose 1.8 再合并

### 决策 2：Navigation 降级策略

鸿蒙端使用 Compose 1.6.1，Navigation 3 不可用。选项：
- 降级到 Navigation 2.x
- 自研简易路由（state 驱动）

### 决策 3：iOS 是否一起用 ovCompose

ovCompose 同时解决"iOS 混排受限"问题。如果未来 iOS 需要 WebView + Compose 混排，可以一起升级。否则 iOS 继续用官方 Compose。

## 六、分阶段实施计划

### Phase 1：技术验证（0.5-1 天）

目标：最小验证，不碰现有代码。

- 克隆 `ovCompose-sample` 在本机跑通鸿蒙模拟器 Demo
- 安装 DevEco Studio 5.1+、HarmonyOS SDK API 18+
- 申请华为开发者账号（模拟器可免签名）
- 确认 `skikobridge.har` 和 `compose.har` 能成功从 sample 中提取

**验收标准**：Hello Compose 在鸿蒙模拟器上显示。

### Phase 2：建立鸿蒙构建目录，共享模块添加 ohosArm64 target（1-2 天）

- 在 `mobile/gradle/` 新建 `libs.ohos.versions.toml`，专为鸿蒙构建使用 KBA 版本
- 按方案 B 新建 `composeHarmonyShared/` 模块
- 给 `core/common`、`core/model`、`core/network`、`core/ui` 和 `feature/*` 添加 `ohosArm64()` target
- 适配纯 commonMain 代码能编译到鸿蒙

**风险排查**：feature 模块里的 `androidx.lifecycle.viewmodel`、`coil3`、`sqldelight` 等库需逐个确认是否支持 ohosArm64。

### Phase 3：平台差异化实现（2-3 天）

#### 3.1 HTTP Engine

`core/network/src/ohosArm64Main/`：

```kotlin
// HttpEngineFactory.ohosArm64.kt
actual fun createHttpEngine(): HttpClientEngine = ...
```

需确认 Ktor 是否支持 `ohosArm64` target。若不支持：
- fallback 用 `libcurl` 的 Kotlin 绑定
- 或走 NAPI 桥到鸿蒙 `@ohos.net.http` 再回调回 Kotlin

#### 3.2 Token 存储

DataStore 不支持 ohosArm64。替代方案：

```kotlin
// TokenStorage.ohosArm64.kt
class OhosTokenStorage : TokenProvider {
    // 通过 NAPI 调 @ohos.data.preferences 或直接写文件
}
```

`AuthExpiredBus` 是纯 Kotlin 实现，可直接复用。

#### 3.3 图片加载

Coil3 对 ohosArm64 支持不确定：
- 确认支持则直接用
- 不支持则定义 `expect fun AsyncImage(url, ...)`，鸿蒙端走 `Image($rawfile())` 或自研下载

#### 3.4 图片选择器

`composeApp/picker/rememberImagePickerLauncher` 需要 `ohosArm64Main` 实现，调 `@ohos.file.picker`。

### Phase 4：导出 ArkUIViewController（1 天）

在 `composeHarmonyShared/` 模块：

```kotlin
// src/ohosArm64Main/kotlin/.../HarmonyEntry.kt
@CName("createMainArkUIViewController")
fun createMainArkUIViewController(env: napi_env): napi_value {
    initMainHandler(env)
    val appComponent = AppComponent(getOhosPreferencesPath())
    return ComposeArkUIViewController(env) {
        CulinoNavHost(appComponent = appComponent)
    }
}
```

`build.gradle.kts` 配置：

```kotlin
kotlin {
    ohosArm64 {
        binaries.sharedLib {
            baseName = "kn"
            linkerOpts("-L${projectDir}/libs/", "-lskia")
            export(libs.compose.multiplatform.export)
        }
    }
}

arrayOf("debug", "release").forEach { type ->
    tasks.register<Copy>("publish${type.capitalizeUS()}BinariesToHarmonyApp") {
        group = "harmony"
        dependsOn("link${type.capitalizeUS()}SharedOhosArm64")
        into(rootProject.file("harmonyApp"))
        from("build/bin/ohosArm64/${type}Shared/libkn_api.h") {
            into("entry/src/main/cpp/include/")
        }
        from("build/bin/ohosArm64/${type}Shared/libkn.so") {
            into("entry/libs/arm64-v8a/")
        }
    }
}
```

### Phase 5：鸿蒙壳工程（1 天）

1. 用 DevEco Studio 新建 `harmonyApp/`（Native C++ template）
2. 配置 `oh-package.json5` 依赖 compose.har + skikobridge.har：

```json5
{
  "dependencies": {
    "libentry.so": "file:./src/main/cpp/types/libentry",
    "compose": "file:./libs/compose.har",
    "skikobridge": "file:./libs/skikobridge.har"
  }
}
```

3. `CMakeLists.txt` 链接 libkn.so、skikobridge：

```cmake
cmake_minimum_required(VERSION 3.5.0)
project(harmonyApp)
set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})

add_definitions(-std=c++17)
include_directories(${NATIVERENDER_ROOT_PATH} ${NATIVERENDER_ROOT_PATH}/include)

find_package(skikobridge)
add_library(entry SHARED napi_init.cpp)
target_link_libraries(entry PUBLIC libace_napi.z.so)
target_link_libraries(entry PUBLIC ${NATIVERENDER_ROOT_PATH}/../../../libs/arm64-v8a/libkn.so)
target_link_libraries(entry PUBLIC skikobridge::skikobridge)
target_link_libraries(entry PUBLIC ${EGL-lib} ${GLES-lib} ${hilog-lib} ${libace-lib} ${libnapi-lib} ${libuv-lib} libc++_shared.so)
```

4. `napi_init.cpp` 调用 Kotlin 导出的创建函数：

```cpp
static napi_value CreateMainArkUIViewController(napi_env env, napi_callback_info info) {
    auto controller = createMainArkUIViewController(env);
    return reinterpret_cast<napi_value>(controller);
}

static napi_value Init(napi_env env, napi_value exports) {
    androidx_compose_ui_arkui_init(env, exports);
    napi_property_descriptor desc[] = {
        {"createMainArkUIViewController", nullptr, CreateMainArkUIViewController,
         nullptr, nullptr, nullptr, napi_default, nullptr}
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    return exports;
}
```

5. ArkTS 入口页面嵌 Compose 组件：

```typescript
import { ArkUIViewController, Compose } from 'compose';
import { createMainArkUIViewController } from 'libentry.so';

@Entry
@Component
struct MainPage {
  private controller: ArkUIViewController = createMainArkUIViewController();

  onPageShow(): void { this.controller.onPageShow(); }
  onPageHide(): void { this.controller.onPageHide(); }
  onBackPress(): boolean | void { return this.controller.onBackPress(); }

  build() {
    Stack() {
      Compose({ controller: this.controller, libraryName: "entry" })
    }.width('100%').height('100%')
  }
}
```

6. 模拟器签名：`File → Project Structure → Signing Configs → 自动签名`

### Phase 6：适配与回归（2-3 天）

- UI 适配：系统字体、状态栏、刘海、返回手势
- API 联调：网络请求能打到后端
- Material Icons：ovCompose 基于 1.6.1 fork，确认 `materialIconsExtended` 是否可用
- 导航改写：Navigation 3 → Navigation 2 或自研
- 图片上传/下载全链路验证
- 401 自动跳转登录流程在鸿蒙端验证

## 七、风险清单

| 风险 | 可能性 | 影响 | 缓解策略 |
|---|---|---|---|
| 全项目降级 Kotlin，三方库不兼容 | 高 | 高 | 采用方案 B 物理隔离 |
| Compose 1.8→1.6 丢失 API | 高 | 中 | 鸿蒙端独立 UI 栈，不强求共享 |
| Ktor 不支持 ohosArm64 | 中 | 高 | 自建 HTTP 桥或 libcurl |
| DataStore 不支持鸿蒙 | 高 | 中 | 自定义 TokenProvider 走 NAPI |
| Coil 图片加载不支持鸿蒙 | 中 | 中 | expect/actual 自己实现 |
| SQLDelight 不支持鸿蒙 | 中 | 中 | 评估是否使用，替代方案 |
| skikobridge.har 需手动从 sample 提取 | 低 | 低 | 固定版本到 repo |
| DevEco 签名配置复杂 | 低 | 低 | 模拟器免签 |
| Kotlin 协程 1.8-KBA 版本 API 偏老 | 低 | 低 | 避免用新 Flow API |

## 八、推荐路径

**方案 B + 先 Phase 1 验证**：

1. 先把 `ovCompose-sample` 跑通，确认环境 OK
2. 新建 `composeHarmonyShared/` 和 `harmonyApp/` 物理隔离现有代码
3. 逐模块适配 `ohosArm64` target
4. 首期允许 UI 代码部分复制
5. 等 ovCompose 追上官方版本再合并

这样现有 Android/iOS 零风险，鸿蒙端独立演进。

## 九、参考资料

- ovCompose-sample: https://github.com/Tencent-TDS/ovCompose-sample
- ovCompose-multiplatform-core: https://github.com/Tencent-TDS/ovCompose-multiplatform-core
- KuiklyBase-platform: https://github.com/Tencent-TDS/KuiklyBase-platform
- HarmonyOS Developer: https://developer.huawei.com/consumer/cn/deveco-studio/
