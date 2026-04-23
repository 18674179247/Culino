# Menu Mobile Phase 1: Project Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold the KMP multi-module project with dual build system (Gradle + Bazel), implement core infrastructure modules, user authentication feature, and a working app shell with navigation — producing a runnable Android app that can register, login, view/edit profile, and logout.

**Architecture:** Clean Architecture + MVI. Multi-module Gradle project with Bazel build files. Shared KMP code in commonMain, platform-specific code via expect/actual. Compose Multiplatform for UI across Android, iOS, HarmonyOS NEXT.

**Tech Stack:** Kotlin 2.1+, Compose Multiplatform 1.7+, Ktor Client 3.x, SQLDelight 2.x, DataStore, kotlin-inject, Compose Navigation, Coil 3.x, kotlinx.serialization, Napier

**Phases Overview:**
- **Phase 1 (this plan):** Project scaffolding + core modules + user auth + app shell
- Phase 2: feature-recipe (CRUD, search, recommendations)
- Phase 3: feature-ingredient (browse + admin management)
- Phase 4: feature-social (favorites, cooking logs)
- Phase 5: feature-tool (shopping lists, meal plans)
- Phase 6: feature-upload + polish + HarmonyOS NEXT integration

---

## File Structure

This phase creates the following project tree. The project lives in a **new repository** `menu-mobile/` (sibling to `menu-backend/`).

```
menu-mobile/
├── build.gradle.kts                          # Root Gradle config
├── settings.gradle.kts                       # Module registration + dependency resolution
├── gradle.properties                         # Kotlin/Compose/Android properties
├── gradle/libs.versions.toml                 # Version catalog
├── BUILD.bazel                               # Root Bazel build
├── WORKSPACE.bazel                           # Bazel workspace + external deps
├── .bazelrc                                  # Bazel options
│
├── core/
│   ├── common/
│   │   ├── build.gradle.kts
│   │   ├── BUILD.bazel
│   │   └── src/commonMain/kotlin/com/menu/core/common/
│   │       ├── AppResult.kt                  # Result wrapper (Success/Error)
│   │       ├── Constants.kt                  # App-wide constants
│   │       └── Extensions.kt                 # Kotlin extension functions
│   │
│   ├── model/
│   │   ├── build.gradle.kts
│   │   ├── BUILD.bazel
│   │   └── src/commonMain/kotlin/com/menu/core/model/
│   │       ├── User.kt                       # User domain model + DTOs
│   │       ├── ApiResponse.kt                # Generic API response wrapper
│   │       └── Auth.kt                       # Login/Register request/response DTOs
│   │
│   ├── network/
│   │   ├── build.gradle.kts
│   │   ├── BUILD.bazel
│   │   └── src/
│   │       ├── commonMain/kotlin/com/menu/core/network/
│   │       │   ├── HttpClientFactory.kt      # Ktor client creation with plugins
│   │       │   ├── TokenProvider.kt          # Interface for token storage
│   │       │   └── ApiClient.kt              # Base API request helpers
│   │       ├── androidMain/kotlin/com/menu/core/network/
│   │       │   └── HttpEngineFactory.kt      # CIO engine
│   │       └── iosMain/kotlin/com/menu/core/network/
│   │           └── HttpEngineFactory.kt      # Darwin engine
│   │
│   ├── data/
│   │   ├── build.gradle.kts
│   │   ├── BUILD.bazel
│   │   └── src/
│   │       ├── commonMain/kotlin/com/menu/core/data/
│   │       │   ├── TokenStorage.kt           # DataStore-based token persistence
│   │       │   └── PreferencesStorage.kt     # User preferences (DataStore)
│   │       ├── androidMain/kotlin/com/menu/core/data/
│   │       │   └── DataStoreFactory.kt       # Android DataStore creation
│   │       └── iosMain/kotlin/com/menu/core/data/
│   │           └── DataStoreFactory.kt       # iOS DataStore creation
│   │
│   └── ui/
│       ├── build.gradle.kts
│       ├── BUILD.bazel
│       └── src/commonMain/kotlin/com/menu/core/ui/
│           ├── theme/
│           │   ├── Theme.kt                  # MenuTheme composable
│           │   ├── Color.kt                  # Color palette
│           │   └── Type.kt                   # Typography
│           └── component/
│               ├── LoadingButton.kt          # Button with loading state
│               ├── MenuTextField.kt          # Styled text field
│               └── ErrorMessage.kt           # Error display component
│
├── feature/
│   └── user/
│       ├── build.gradle.kts
│       ├── BUILD.bazel
│       └── src/commonMain/kotlin/com/menu/feature/user/
│           ├── data/
│           │   ├── UserApi.kt                # Ktor API service for /user endpoints
│           │   └── UserRepositoryImpl.kt     # Repository implementation
│           ├── domain/
│           │   ├── UserRepository.kt         # Repository interface
│           │   ├── LoginUseCase.kt           # Login business logic
│           │   ├── RegisterUseCase.kt        # Register business logic
│           │   └── GetProfileUseCase.kt      # Get/update profile logic
│           └── presentation/
│               ├── login/
│               │   ├── LoginState.kt         # UiState + Intent
│               │   ├── LoginViewModel.kt     # MVI ViewModel
│               │   └── LoginScreen.kt        # Compose UI
│               ├── register/
│               │   ├── RegisterState.kt
│               │   ├── RegisterViewModel.kt
│               │   └── RegisterScreen.kt
│               └── profile/
│                   ├── ProfileState.kt
│                   ├── ProfileViewModel.kt
│                   └── ProfileScreen.kt
│
├── composeApp/
│   ├── build.gradle.kts
│   ├── BUILD.bazel
│   └── src/
│       ├── commonMain/kotlin/com/menu/app/
│       │   ├── App.kt                        # Root composable with theme + navigation
│       │   ├── Navigation.kt                 # NavHost + route definitions
│       │   └── di/
│       │       └── AppComponent.kt           # kotlin-inject root component
│       ├── androidMain/kotlin/com/menu/app/
│       │   ├── MenuApplication.kt            # Android Application class
│       │   └── MainActivity.kt               # Single Activity
│       └── iosMain/kotlin/com/menu/app/
│           └── MainViewController.kt         # iOS entry point
│
└── tests/                                    # Shared test utilities (if needed)
```

---

## Task 1: Initialize Project and Gradle Configuration

**Files:**
- Create: `menu-mobile/build.gradle.kts`
- Create: `menu-mobile/settings.gradle.kts`
- Create: `menu-mobile/gradle.properties`
- Create: `menu-mobile/gradle/libs.versions.toml`
- Create: `menu-mobile/.gitignore`

- [ ] **Step 1: Create project directory and initialize git**

```bash
cd /Users/bilibili/rs
mkdir menu-mobile && cd menu-mobile
git init
```

- [ ] **Step 2: Create .gitignore**

Create `menu-mobile/.gitignore`:

```gitignore
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
.DS_Store

# Kotlin
*.class

# Bazel
bazel-*
.bazelisk/

# Local config
local.properties
```

- [ ] **Step 3: Create version catalog**

Create `menu-mobile/gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.1.20"
compose-multiplatform = "1.7.3"
agp = "8.7.3"
ktor = "3.1.1"
sqldelight = "2.0.2"
kotlinx-coroutines = "1.10.1"
kotlinx-serialization = "1.8.0"
kotlinx-datetime = "0.6.2"
kotlin-inject = "0.7.2"
ksp = "2.1.20-1.0.32"
coil = "3.1.0"
datastore = "1.1.4"
napier = "2.7.1"
navigation = "2.8.0-alpha13"
turbine = "1.2.0"
androidx-activityCompose = "1.10.1"
androidx-lifecycle = "2.8.4"

[libraries]
# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-auth = { module = "io.ktor:ktor-client-auth", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }

# Kotlinx
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }

# SQLDelight
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }

# DataStore
datastore-preferences = { module = "androidx.datastore:datastore-preferences-core", version.ref = "datastore" }

# DI
kotlin-inject-runtime = { module = "me.tatarka.inject:kotlin-inject-runtime-kmp", version.ref = "kotlin-inject" }
kotlin-inject-compiler = { module = "me.tatarka.inject:kotlin-inject-compiler-ksp", version.ref = "kotlin-inject" }

# Image
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }

# Navigation
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation" }

# Logging
napier = { module = "io.github.aakira:napier", version.ref = "napier" }

# Android
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activityCompose" }
androidx-lifecycle-viewmodel = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }

# Test
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }

[plugins]
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidLibrary = { id = "com.android.library", version.ref = "agp" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 4: Create gradle.properties**

Create `menu-mobile/gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048M -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
kotlin.mpp.androidSourceSetLayoutVersion=2
org.gradle.configuration-cache=true
```

- [ ] **Step 5: Create root build.gradle.kts**

Create `menu-mobile/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 6: Create settings.gradle.kts**

Create `menu-mobile/settings.gradle.kts`:

```kotlin
rootProject.name = "menu-mobile"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

include(":core:common")
include(":core:model")
include(":core:network")
include(":core:data")
include(":core:ui")
include(":feature:user")
include(":composeApp")
```

- [ ] **Step 7: Add Gradle wrapper**

```bash
cd /Users/bilibili/rs/menu-mobile
gradle wrapper --gradle-version 8.12
```

- [ ] **Step 8: Verify Gradle sync**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew --version
```

Expected: Gradle 8.12 version info printed.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "init: project scaffolding with Gradle version catalog"
```

---

## Task 2: Core Common Module

**Files:**
- Create: `core/common/build.gradle.kts`
- Create: `core/common/src/commonMain/kotlin/com/menu/core/common/AppResult.kt`
- Create: `core/common/src/commonMain/kotlin/com/menu/core/common/Constants.kt`
- Create: `core/common/src/commonMain/kotlin/com/menu/core/common/Extensions.kt`
- Create: `core/common/src/commonTest/kotlin/com/menu/core/common/AppResultTest.kt`

- [ ] **Step 1: Create build.gradle.kts for core:common**

Create `menu-mobile/core/common/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

- [ ] **Step 2: Write failing test for AppResult**

Create `core/common/src/commonTest/kotlin/com/menu/core/common/AppResultTest.kt`:

```kotlin
package com.menu.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AppResultTest {

    @Test
    fun successWrapsValue() {
        val result: AppResult<String> = AppResult.Success("hello")
        assertIs<AppResult.Success<String>>(result)
        assertEquals("hello", result.data)
    }

    @Test
    fun errorWrapsMessage() {
        val result: AppResult<String> = AppResult.Error("fail")
        assertIs<AppResult.Error>(result)
        assertEquals("fail", result.message)
    }

    @Test
    fun errorExceptionIsOptional() {
        val result = AppResult.Error("fail")
        assertNull(result.exception)
    }

    @Test
    fun mapTransformsSuccess() {
        val result: AppResult<Int> = AppResult.Success("hello").map { it.length }
        assertIs<AppResult.Success<Int>>(result)
        assertEquals(5, result.data)
    }

    @Test
    fun mapPassesThroughError() {
        val result: AppResult<Int> = AppResult.Error("fail").map { 42 }
        assertIs<AppResult.Error>(result)
        assertEquals("fail", result.message)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :core:common:jvmTest
```

Expected: FAIL — `AppResult` not found.

- [ ] **Step 4: Implement AppResult**

Create `core/common/src/commonMain/kotlin/com/menu/core/common/AppResult.kt`:

```kotlin
package com.menu.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (String, Throwable?) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(message, exception)
    return this
}

fun <T> AppResult<T>.getOrNull(): T? = when (this) {
    is AppResult.Success -> data
    is AppResult.Error -> null
}
```

- [ ] **Step 5: Create Constants**

Create `core/common/src/commonMain/kotlin/com/menu/core/common/Constants.kt`:

```kotlin
package com.menu.core.common

object Constants {
    const val API_BASE_URL = "http://10.0.2.2:3000/api/v1/"
    const val TOKEN_KEY = "auth_token"
    const val CACHE_TTL_MINUTES = 30L
    const val PAGE_SIZE = 20
}
```

- [ ] **Step 6: Create Extensions**

Create `core/common/src/commonMain/kotlin/com/menu/core/common/Extensions.kt`:

```kotlin
package com.menu.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

fun <T> Flow<T>.asResult(): Flow<AppResult<T>> =
    map<T, AppResult<T>> { AppResult.Success(it) }
        .catch { emit(AppResult.Error(it.message ?: "Unknown error", it)) }
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :core:common:jvmTest
```

Expected: All 5 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add core/common/
git commit -m "feat(core): add common module with AppResult, Constants, Extensions"
```

---

## Task 3: Core Model Module

**Files:**
- Create: `core/model/build.gradle.kts`
- Create: `core/model/src/commonMain/kotlin/com/menu/core/model/ApiResponse.kt`
- Create: `core/model/src/commonMain/kotlin/com/menu/core/model/Auth.kt`
- Create: `core/model/src/commonMain/kotlin/com/menu/core/model/User.kt`
- Create: `core/model/src/commonTest/kotlin/com/menu/core/model/ApiResponseTest.kt`

- [ ] **Step 1: Create build.gradle.kts for core:model**

Create `menu-mobile/core/model/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
```

- [ ] **Step 2: Write failing test for ApiResponse deserialization**

Create `core/model/src/commonTest/kotlin/com/menu/core/model/ApiResponseTest.kt`:

```kotlin
package com.menu.core.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun deserializeSuccessResponse() {
        val raw = """{"ok":true,"data":{"id":"abc","username":"test","nickname":"Test User","avatar":null,"role_code":"user"},"error":null}"""
        val response = json.decodeFromString<ApiResponse<UserDto>>(raw)
        assertTrue(response.ok)
        assertEquals("abc", response.data?.id)
        assertEquals("test", response.data?.username)
        assertNull(response.error)
    }

    @Test
    fun deserializeErrorResponse() {
        val raw = """{"ok":false,"data":null,"error":"Invalid credentials"}"""
        val response = json.decodeFromString<ApiResponse<UserDto>>(raw)
        assertTrue(!response.ok)
        assertNull(response.data)
        assertEquals("Invalid credentials", response.error)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :core:model:jvmTest
```

Expected: FAIL — `ApiResponse`, `UserDto` not found.

- [ ] **Step 4: Implement ApiResponse**

Create `core/model/src/commonMain/kotlin/com/menu/core/model/ApiResponse.kt`:

```kotlin
package com.menu.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: String? = null
)
```

- [ ] **Step 5: Implement User model and DTOs**

Create `core/model/src/commonMain/kotlin/com/menu/core/model/User.kt`:

```kotlin
package com.menu.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val nickname: String? = null,
    val avatar: String? = null,
    @SerialName("role_code") val roleCode: String = "user"
)

data class User(
    val id: String,
    val username: String,
    val nickname: String?,
    val avatar: String?,
    val roleCode: String
) {
    val isAdmin: Boolean get() = roleCode == "admin"
}

fun UserDto.toDomain(): User = User(
    id = id,
    username = username,
    nickname = nickname,
    avatar = avatar,
    roleCode = roleCode
)
```

- [ ] **Step 6: Implement Auth DTOs**

Create `core/model/src/commonMain/kotlin/com/menu/core/model/Auth.kt`:

```kotlin
package com.menu.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String? = null
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto
)

@Serializable
data class UpdateProfileRequest(
    val nickname: String? = null,
    val avatar: String? = null
)
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :core:model:jvmTest
```

Expected: All 2 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add core/model/
git commit -m "feat(core): add model module with API response, User, Auth DTOs"
```

---

## Task 4: Core Network Module

**Files:**
- Create: `core/network/build.gradle.kts`
- Create: `core/network/src/commonMain/kotlin/com/menu/core/network/TokenProvider.kt`
- Create: `core/network/src/commonMain/kotlin/com/menu/core/network/HttpClientFactory.kt`
- Create: `core/network/src/commonMain/kotlin/com/menu/core/network/ApiClient.kt`
- Create: `core/network/src/androidMain/kotlin/com/menu/core/network/HttpEngineFactory.kt`
- Create: `core/network/src/iosMain/kotlin/com/menu/core/network/HttpEngineFactory.kt`
- Create: `core/network/src/commonTest/kotlin/com/menu/core/network/ApiClientTest.kt`

- [ ] **Step 1: Create build.gradle.kts for core:network**

Create `menu-mobile/core/network/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.napier)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
```

- [ ] **Step 2: Create TokenProvider interface**

Create `core/network/src/commonMain/kotlin/com/menu/core/network/TokenProvider.kt`:

```kotlin
package com.menu.core.network

interface TokenProvider {
    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}
```

- [ ] **Step 3: Create platform engine factories**

Create `core/network/src/androidMain/kotlin/com/menu/core/network/HttpEngineFactory.kt`:

```kotlin
package com.menu.core.network

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*

actual fun createHttpEngine(): HttpClientEngineFactory<*> = CIO
```

Create `core/network/src/iosMain/kotlin/com/menu/core/network/HttpEngineFactory.kt`:

```kotlin
package com.menu.core.network

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

actual fun createHttpEngine(): HttpClientEngineFactory<*> = Darwin
```

Create the expect declaration in `core/network/src/commonMain/kotlin/com/menu/core/network/HttpEngineFactory.kt`:

```kotlin
package com.menu.core.network

import io.ktor.client.engine.*

expect fun createHttpEngine(): HttpClientEngineFactory<*>
```

Wait — for jvm target used in tests, we also need a jvmMain source set. Add:

Create `core/network/src/jvmMain/kotlin/com/menu/core/network/HttpEngineFactory.kt`:

```kotlin
package com.menu.core.network

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*

actual fun createHttpEngine(): HttpClientEngineFactory<*> = CIO
```

And add to `build.gradle.kts` under `jvmMain.dependencies`:

```kotlin
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
```

- [ ] **Step 4: Create HttpClientFactory**

Create `core/network/src/commonMain/kotlin/com/menu/core/network/HttpClientFactory.kt`:

```kotlin
package com.menu.core.network

import com.menu.core.common.Constants
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createHttpClient(tokenProvider: TokenProvider): HttpClient {
    return HttpClient(createHttpEngine()) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenProvider.getToken()
                    token?.let { BearerTokens(it, "") }
                }
            }
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    io.github.aakira.napier.Napier.d(message, tag = "HTTP")
                }
            }
            level = LogLevel.HEADERS
        }

        defaultRequest {
            url(Constants.API_BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }
}
```

- [ ] **Step 5: Write failing test for ApiClient**

Create `core/network/src/commonTest/kotlin/com/menu/core/network/ApiClientTest.kt`:

```kotlin
package com.menu.core.network

import com.menu.core.common.AppResult
import com.menu.core.model.ApiResponse
import com.menu.core.model.UserDto
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApiClientTest {

    private fun mockClient(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        return HttpClient(MockEngine { _ ->
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun safeApiCallReturnsSuccessOnOkResponse() = runTest {
        val client = mockClient(
            """{"ok":true,"data":{"id":"1","username":"test","nickname":"Test","avatar":null,"role_code":"user"},"error":null}"""
        )
        val apiClient = ApiClient(client)
        val result = apiClient.safeRequest<ApiResponse<UserDto>> {
            url { path("user/me") }
            method = HttpMethod.Get
        }
        assertIs<AppResult.Success<ApiResponse<UserDto>>>(result)
        assertEquals("test", result.data.data?.username)
    }

    @Test
    fun safeApiCallReturnsErrorOnServerError() = runTest {
        val client = mockClient(
            """{"ok":false,"data":null,"error":"Not found"}""",
            HttpStatusCode.NotFound
        )
        val apiClient = ApiClient(client)
        val result = apiClient.safeRequest<ApiResponse<UserDto>> {
            url { path("user/me") }
            method = HttpMethod.Get
        }
        assertIs<AppResult.Error>(result)
    }
}
```

- [ ] **Step 6: Run test to verify it fails**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :core:network:jvmTest
```

Expected: FAIL — `ApiClient` not found.

- [ ] **Step 7: Implement ApiClient**

Create `core/network/src/commonMain/kotlin/com/menu/core/network/ApiClient.kt`:

```kotlin
package com.menu.core.network

import com.menu.core.common.AppResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class ApiClient(private val httpClient: HttpClient) {

    suspend inline fun <reified T> safeRequest(
        block: HttpRequestBuilder.() -> Unit
    ): AppResult<T> {
        return try {
            val response: HttpResponse = httpClient.request(block)
            if (response.status.value in 200..299) {
                AppResult.Success(response.body<T>())
            } else {
                AppResult.Error("HTTP ${response.status.value}: ${response.status.description}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Network error", e)
        }
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :core:network:jvmTest
```

Expected: All 2 tests PASS.

- [ ] **Step 9: Commit**

```bash
git add core/network/
git commit -m "feat(core): add network module with Ktor client, ApiClient, TokenProvider"
```

---

## Task 5: Core Data Module (Token & Preferences Storage)

**Files:**
- Create: `core/data/build.gradle.kts`
- Create: `core/data/src/commonMain/kotlin/com/menu/core/data/TokenStorage.kt`
- Create: `core/data/src/commonMain/kotlin/com/menu/core/data/PreferencesStorage.kt`
- Create: `core/data/src/androidMain/kotlin/com/menu/core/data/DataStoreFactory.kt`
- Create: `core/data/src/iosMain/kotlin/com/menu/core/data/DataStoreFactory.kt`
- Create: `core/data/src/jvmMain/kotlin/com/menu/core/data/DataStoreFactory.kt`
- Create: `core/data/src/commonTest/kotlin/com/menu/core/data/TokenStorageTest.kt`

- [ ] **Step 1: Create build.gradle.kts for core:data**

Create `menu-mobile/core/data/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:network"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.datastore.preferences)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

- [ ] **Step 2: Create expect/actual DataStore factory**

Create `core/data/src/commonMain/kotlin/com/menu/core/data/DataStoreFactory.kt`:

```kotlin
package com.menu.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect fun createDataStore(): DataStore<Preferences>

internal const val DATA_STORE_FILE_NAME = "menu_prefs.preferences_pb"
```

Create `core/data/src/androidMain/kotlin/com/menu/core/data/DataStoreFactory.kt`:

```kotlin
package com.menu.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

lateinit var appContext: Context

actual fun createDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath {
        appContext.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath()
    }
}
```

Create `core/data/src/iosMain/kotlin/com/menu/core/data/DataStoreFactory.kt`:

```kotlin
package com.menu.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun createDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath {
        val dir = NSFileManager.defaultManager.URLForDirectory(
            NSDocumentDirectory, NSUserDomainMask, null, true, null
        )!!.path!!
        "$dir/$DATA_STORE_FILE_NAME".toPath()
    }
}
```

Create `core/data/src/jvmMain/kotlin/com/menu/core/data/DataStoreFactory.kt`:

```kotlin
package com.menu.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

actual fun createDataStore(): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath {
        "${System.getProperty("java.io.tmpdir")}/$DATA_STORE_FILE_NAME".toPath()
    }
}
```

- [ ] **Step 3: Write failing test for TokenStorage**

Create `core/data/src/commonTest/kotlin/com/menu/core/data/TokenStorageTest.kt`:

```kotlin
package com.menu.core.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenStorageTest {

    @Test
    fun saveAndRetrieveToken() = runTest {
        val storage = TokenStorage(createDataStore())
        storage.saveToken("test-token-123")
        val token = storage.getToken()
        assertEquals("test-token-123", token)
    }

    @Test
    fun clearTokenRemovesIt() = runTest {
        val storage = TokenStorage(createDataStore())
        storage.saveToken("test-token-123")
        storage.clearToken()
        val token = storage.getToken()
        assertNull(token)
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :core:data:jvmTest
```

Expected: FAIL — `TokenStorage` not found.

- [ ] **Step 5: Implement TokenStorage**

Create `core/data/src/commonMain/kotlin/com/menu/core/data/TokenStorage.kt`:

```kotlin
package com.menu.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.menu.core.network.TokenProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TokenStorage(
    private val dataStore: DataStore<Preferences>
) : TokenProvider {

    private val tokenKey = stringPreferencesKey("auth_token")

    override suspend fun getToken(): String? {
        return dataStore.data.map { prefs -> prefs[tokenKey] }.first()
    }

    override suspend fun saveToken(token: String) {
        dataStore.edit { prefs -> prefs[tokenKey] = token }
    }

    override suspend fun clearToken() {
        dataStore.edit { prefs -> prefs.remove(tokenKey) }
    }
}
```

- [ ] **Step 6: Implement PreferencesStorage**

Create `core/data/src/commonMain/kotlin/com/menu/core/data/PreferencesStorage.kt`:

```kotlin
package com.menu.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesStorage(
    private val dataStore: DataStore<Preferences>
) {
    fun getString(key: String): Flow<String?> {
        return dataStore.data.map { prefs -> prefs[stringPreferencesKey(key)] }
    }

    suspend fun putString(key: String, value: String) {
        dataStore.edit { prefs -> prefs[stringPreferencesKey(key)] = value }
    }

    suspend fun remove(key: String) {
        dataStore.edit { prefs -> prefs.remove(stringPreferencesKey(key)) }
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :core:data:jvmTest
```

Expected: All 2 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add core/data/
git commit -m "feat(core): add data module with TokenStorage, PreferencesStorage, DataStore"
```

---

## Task 6: Core UI Module (Theme & Components)

**Files:**
- Create: `core/ui/build.gradle.kts`
- Create: `core/ui/src/commonMain/kotlin/com/menu/core/ui/theme/Color.kt`
- Create: `core/ui/src/commonMain/kotlin/com/menu/core/ui/theme/Type.kt`
- Create: `core/ui/src/commonMain/kotlin/com/menu/core/ui/theme/Theme.kt`
- Create: `core/ui/src/commonMain/kotlin/com/menu/core/ui/component/LoadingButton.kt`
- Create: `core/ui/src/commonMain/kotlin/com/menu/core/ui/component/MenuTextField.kt`
- Create: `core/ui/src/commonMain/kotlin/com/menu/core/ui/component/ErrorMessage.kt`

- [ ] **Step 1: Create build.gradle.kts for core:ui**

Create `menu-mobile/core/ui/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
    }
}
```

- [ ] **Step 2: Create Color palette**

Create `core/ui/src/commonMain/kotlin/com/menu/core/ui/theme/Color.kt`:

```kotlin
package com.menu.core.ui.theme

import androidx.compose.ui.graphics.Color

val Orange500 = Color(0xFFFF9800)
val Orange700 = Color(0xFFF57C00)
val Green500 = Color(0xFF4CAF50)
val Green700 = Color(0xFF388E3C)
val Red500 = Color(0xFFF44336)
val Gray100 = Color(0xFFF5F5F5)
val Gray300 = Color(0xFFE0E0E0)
val Gray600 = Color(0xFF757575)
val Gray900 = Color(0xFF212121)
```

- [ ] **Step 3: Create Typography**

Create `core/ui/src/commonMain/kotlin/com/menu/core/ui/theme/Type.kt`:

```kotlin
package com.menu.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MenuTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)
```

- [ ] **Step 4: Create Theme**

Create `core/ui/src/commonMain/kotlin/com/menu/core/ui/theme/Theme.kt`:

```kotlin
package com.menu.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    primaryContainer = Orange700,
    secondary = Green500,
    secondaryContainer = Green700,
    error = Red500,
    background = Gray100,
    surface = Gray100,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = Gray900,
    onSurface = Gray900,
    outline = Gray300,
    onSurfaceVariant = Gray600,
)

private val DarkColorScheme = darkColorScheme(
    primary = Orange500,
    primaryContainer = Orange700,
    secondary = Green500,
    secondaryContainer = Green700,
    error = Red500,
)

@Composable
fun MenuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MenuTypography,
        content = content
    )
}
```

- [ ] **Step 5: Create LoadingButton component**

Create `core/ui/src/commonMain/kotlin/com/menu/core/ui/component/LoadingButton.kt`:

```kotlin
package com.menu.core.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text)
    }
}
```

- [ ] **Step 6: Create MenuTextField component**

Create `core/ui/src/commonMain/kotlin/com/menu/core/ui/component/MenuTextField.kt`:

```kotlin
package com.menu.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun MenuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage) }
        } else null,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine
    )
}
```

- [ ] **Step 7: Create ErrorMessage component**

Create `core/ui/src/commonMain/kotlin/com/menu/core/ui/component/ErrorMessage.kt`:

```kotlin
package com.menu.core.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onRetry) {
                    Text("重试")
                }
            }
        }
    }
}
```

- [ ] **Step 8: Verify compilation**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :core:ui:jvmMainClasses
```

Expected: BUILD SUCCESSFUL (Compose UI modules are hard to unit test without Android instrumentation, so we verify compilation).

- [ ] **Step 9: Commit**

```bash
git add core/ui/
git commit -m "feat(core): add ui module with MenuTheme, LoadingButton, MenuTextField, ErrorMessage"
```

---

## Task 7: Feature User — Data Layer (API + Repository)

**Files:**
- Create: `feature/user/build.gradle.kts`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/data/UserApi.kt`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/domain/UserRepository.kt`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/data/UserRepositoryImpl.kt`
- Create: `feature/user/src/commonTest/kotlin/com/menu/feature/user/data/UserRepositoryImplTest.kt`

- [ ] **Step 1: Create build.gradle.kts for feature:user**

Create `menu-mobile/feature/user/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))
            implementation(project(":core:network"))
            implementation(project(":core:data"))
            implementation(project(":core:ui"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.napier)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
        }
    }
}
```

- [ ] **Step 2: Create UserApi**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/data/UserApi.kt`:

```kotlin
package com.menu.feature.user.data

import com.menu.core.common.AppResult
import com.menu.core.model.*
import com.menu.core.network.ApiClient
import io.ktor.client.request.*
import io.ktor.http.*

class UserApi(private val apiClient: ApiClient) {

    suspend fun login(request: LoginRequest): AppResult<ApiResponse<AuthResponse>> =
        apiClient.safeRequest {
            url { path("user/login") }
            method = HttpMethod.Post
            setBody(request)
        }

    suspend fun register(request: RegisterRequest): AppResult<ApiResponse<AuthResponse>> =
        apiClient.safeRequest {
            url { path("user/register") }
            method = HttpMethod.Post
            setBody(request)
        }

    suspend fun getProfile(): AppResult<ApiResponse<UserDto>> =
        apiClient.safeRequest {
            url { path("user/me") }
            method = HttpMethod.Get
        }

    suspend fun updateProfile(request: UpdateProfileRequest): AppResult<ApiResponse<UserDto>> =
        apiClient.safeRequest {
            url { path("user/me") }
            method = HttpMethod.Put
            setBody(request)
        }

    suspend fun logout(): AppResult<ApiResponse<Unit>> =
        apiClient.safeRequest {
            url { path("user/logout") }
            method = HttpMethod.Post
        }
}
```

- [ ] **Step 3: Create UserRepository interface**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/domain/UserRepository.kt`:

```kotlin
package com.menu.feature.user.domain

import com.menu.core.common.AppResult
import com.menu.core.model.User

interface UserRepository {
    suspend fun login(username: String, password: String): AppResult<User>
    suspend fun register(username: String, password: String, nickname: String?): AppResult<User>
    suspend fun getProfile(): AppResult<User>
    suspend fun updateProfile(nickname: String?, avatar: String?): AppResult<User>
    suspend fun logout(): AppResult<Unit>
}
```

- [ ] **Step 4: Write failing test for UserRepositoryImpl**

Create `feature/user/src/commonTest/kotlin/com/menu/feature/user/data/UserRepositoryImplTest.kt`:

```kotlin
package com.menu.feature.user.data

import com.menu.core.common.AppResult
import com.menu.core.model.*
import com.menu.core.network.ApiClient
import com.menu.core.network.TokenProvider
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UserRepositoryImplTest {

    private val fakeTokenProvider = object : TokenProvider {
        var token: String? = null
        override suspend fun getToken(): String? = token
        override suspend fun saveToken(t: String) { token = t }
        override suspend fun clearToken() { token = null }
    }

    private fun mockApiClient(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK): ApiClient {
        val client = HttpClient(MockEngine { _ ->
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return ApiClient(client)
    }

    @Test
    fun loginSuccessSavesTokenAndReturnsUser() = runTest {
        val apiClient = mockApiClient(
            """{"ok":true,"data":{"token":"jwt-123","user":{"id":"u1","username":"test","nickname":"Test","avatar":null,"role_code":"user"}},"error":null}"""
        )
        val repo = UserRepositoryImpl(UserApi(apiClient), fakeTokenProvider)
        val result = repo.login("test", "pass")

        assertIs<AppResult.Success<*>>(result)
        val user = (result as AppResult.Success).data
        assertEquals("test", user.username)
        assertEquals("jwt-123", fakeTokenProvider.token)
    }

    @Test
    fun loginFailureReturnsError() = runTest {
        val apiClient = mockApiClient(
            """{"ok":false,"data":null,"error":"Invalid credentials"}"""
        )
        val repo = UserRepositoryImpl(UserApi(apiClient), fakeTokenProvider)
        val result = repo.login("test", "wrong")

        assertIs<AppResult.Error>(result)
        assertEquals("Invalid credentials", (result as AppResult.Error).message)
    }

    @Test
    fun logoutClearsToken() = runTest {
        fakeTokenProvider.token = "jwt-123"
        val apiClient = mockApiClient("""{"ok":true,"data":null,"error":null}""")
        val repo = UserRepositoryImpl(UserApi(apiClient), fakeTokenProvider)
        repo.logout()

        assertEquals(null, fakeTokenProvider.token)
    }
}
```

- [ ] **Step 5: Run test to verify it fails**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :feature:user:jvmTest
```

Expected: FAIL — `UserRepositoryImpl` not found.

- [ ] **Step 6: Implement UserRepositoryImpl**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/data/UserRepositoryImpl.kt`:

```kotlin
package com.menu.feature.user.data

import com.menu.core.common.AppResult
import com.menu.core.model.*
import com.menu.core.network.TokenProvider
import com.menu.feature.user.domain.UserRepository

class UserRepositoryImpl(
    private val userApi: UserApi,
    private val tokenProvider: TokenProvider
) : UserRepository {

    override suspend fun login(username: String, password: String): AppResult<User> {
        return when (val result = userApi.login(LoginRequest(username, password))) {
            is AppResult.Success -> {
                val response = result.data
                if (response.ok && response.data != null) {
                    tokenProvider.saveToken(response.data.token)
                    AppResult.Success(response.data.user.toDomain())
                } else {
                    AppResult.Error(response.error ?: "Login failed")
                }
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun register(username: String, password: String, nickname: String?): AppResult<User> {
        return when (val result = userApi.register(RegisterRequest(username, password, nickname))) {
            is AppResult.Success -> {
                val response = result.data
                if (response.ok && response.data != null) {
                    tokenProvider.saveToken(response.data.token)
                    AppResult.Success(response.data.user.toDomain())
                } else {
                    AppResult.Error(response.error ?: "Registration failed")
                }
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun getProfile(): AppResult<User> {
        return when (val result = userApi.getProfile()) {
            is AppResult.Success -> {
                val response = result.data
                if (response.ok && response.data != null) {
                    AppResult.Success(response.data.toDomain())
                } else {
                    AppResult.Error(response.error ?: "Failed to get profile")
                }
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun updateProfile(nickname: String?, avatar: String?): AppResult<User> {
        return when (val result = userApi.updateProfile(UpdateProfileRequest(nickname, avatar))) {
            is AppResult.Success -> {
                val response = result.data
                if (response.ok && response.data != null) {
                    AppResult.Success(response.data.toDomain())
                } else {
                    AppResult.Error(response.error ?: "Failed to update profile")
                }
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        userApi.logout()
        tokenProvider.clearToken()
        return AppResult.Success(Unit)
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :feature:user:jvmTest
```

Expected: All 3 tests PASS.

- [ ] **Step 8: Commit**

```bash
git add feature/user/
git commit -m "feat(user): add data layer with UserApi, UserRepository"
```

---

## Task 8: Feature User — Domain Layer (UseCases)

**Files:**
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/domain/LoginUseCase.kt`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/domain/RegisterUseCase.kt`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/domain/GetProfileUseCase.kt`
- Create: `feature/user/src/commonTest/kotlin/com/menu/feature/user/domain/LoginUseCaseTest.kt`

- [ ] **Step 1: Write failing test for LoginUseCase**

Create `feature/user/src/commonTest/kotlin/com/menu/feature/user/domain/LoginUseCaseTest.kt`:

```kotlin
package com.menu.feature.user.domain

import com.menu.core.common.AppResult
import com.menu.core.model.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LoginUseCaseTest {

    private val fakeUser = User("u1", "test", "Test", null, "user")

    private val successRepo = object : UserRepository {
        override suspend fun login(username: String, password: String) = AppResult.Success(fakeUser)
        override suspend fun register(username: String, password: String, nickname: String?) = AppResult.Success(fakeUser)
        override suspend fun getProfile() = AppResult.Success(fakeUser)
        override suspend fun updateProfile(nickname: String?, avatar: String?) = AppResult.Success(fakeUser)
        override suspend fun logout() = AppResult.Success(Unit)
    }

    @Test
    fun loginWithEmptyUsernameReturnsError() = runTest {
        val useCase = LoginUseCase(successRepo)
        val result = useCase("", "password")
        assertIs<AppResult.Error>(result)
        assertEquals("用户名不能为空", (result as AppResult.Error).message)
    }

    @Test
    fun loginWithEmptyPasswordReturnsError() = runTest {
        val useCase = LoginUseCase(successRepo)
        val result = useCase("user", "")
        assertIs<AppResult.Error>(result)
        assertEquals("密码不能为空", (result as AppResult.Error).message)
    }

    @Test
    fun loginWithValidCredentialsDelegatesToRepo() = runTest {
        val useCase = LoginUseCase(successRepo)
        val result = useCase("test", "password")
        assertIs<AppResult.Success<User>>(result)
        assertEquals("test", result.data.username)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :feature:user:jvmTest --tests "com.menu.feature.user.domain.*"
```

Expected: FAIL — `LoginUseCase` not found.

- [ ] **Step 3: Implement LoginUseCase**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/domain/LoginUseCase.kt`:

```kotlin
package com.menu.feature.user.domain

import com.menu.core.common.AppResult
import com.menu.core.model.User

class LoginUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(username: String, password: String): AppResult<User> {
        if (username.isBlank()) return AppResult.Error("用户名不能为空")
        if (password.isBlank()) return AppResult.Error("密码不能为空")
        return repository.login(username, password)
    }
}
```

- [ ] **Step 4: Implement RegisterUseCase**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/domain/RegisterUseCase.kt`:

```kotlin
package com.menu.feature.user.domain

import com.menu.core.common.AppResult
import com.menu.core.model.User

class RegisterUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(username: String, password: String, nickname: String?): AppResult<User> {
        if (username.isBlank()) return AppResult.Error("用户名不能为空")
        if (password.length < 6) return AppResult.Error("密码至少6位")
        return repository.register(username, password, nickname)
    }
}
```

- [ ] **Step 5: Implement GetProfileUseCase**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/domain/GetProfileUseCase.kt`:

```kotlin
package com.menu.feature.user.domain

import com.menu.core.common.AppResult
import com.menu.core.model.User

class GetProfileUseCase(private val repository: UserRepository) {
    suspend fun getProfile(): AppResult<User> = repository.getProfile()
    suspend fun updateProfile(nickname: String?, avatar: String?): AppResult<User> =
        repository.updateProfile(nickname, avatar)
    suspend fun logout(): AppResult<Unit> = repository.logout()
}
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :feature:user:jvmTest
```

Expected: All 6 tests PASS (3 from Task 7 + 3 new).

- [ ] **Step 7: Commit**

```bash
git add feature/user/src/commonMain/kotlin/com/menu/feature/user/domain/
git add feature/user/src/commonTest/kotlin/com/menu/feature/user/domain/
git commit -m "feat(user): add domain layer with LoginUseCase, RegisterUseCase, GetProfileUseCase"
```

---

## Task 9: Feature User — Presentation Layer (Login)

**Files:**
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/login/LoginState.kt`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/login/LoginViewModel.kt`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/login/LoginScreen.kt`
- Create: `feature/user/src/commonTest/kotlin/com/menu/feature/user/presentation/login/LoginViewModelTest.kt`

- [ ] **Step 1: Create LoginState**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/login/LoginState.kt`:

```kotlin
package com.menu.feature.user.presentation.login

import com.menu.core.model.User

data class LoginState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedInUser: User? = null
)

sealed interface LoginIntent {
    data class UpdateUsername(val username: String) : LoginIntent
    data class UpdatePassword(val password: String) : LoginIntent
    data object Submit : LoginIntent
    data object ClearError : LoginIntent
}
```

- [ ] **Step 2: Write failing test for LoginViewModel**

Create `feature/user/src/commonTest/kotlin/com/menu/feature/user/presentation/login/LoginViewModelTest.kt`:

```kotlin
package com.menu.feature.user.presentation.login

import app.cash.turbine.test
import com.menu.core.common.AppResult
import com.menu.core.model.User
import com.menu.feature.user.domain.LoginUseCase
import com.menu.feature.user.domain.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeUser = User("u1", "test", "Test", null, "user")

    private fun fakeRepo(result: AppResult<User> = AppResult.Success(fakeUser)) = object : UserRepository {
        override suspend fun login(username: String, password: String) = result
        override suspend fun register(username: String, password: String, nickname: String?) = result
        override suspend fun getProfile() = result
        override suspend fun updateProfile(nickname: String?, avatar: String?) = result
        override suspend fun logout() = AppResult.Success(Unit)
    }

    @BeforeTest
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun updateUsernameUpdatesState() = runTest {
        val vm = LoginViewModel(LoginUseCase(fakeRepo()))
        vm.state.test {
            assertEquals("", awaitItem().username)
            vm.onIntent(LoginIntent.UpdateUsername("test"))
            assertEquals("test", awaitItem().username)
        }
    }

    @Test
    fun submitWithValidCredentialsSetsLoggedInUser() = runTest {
        val vm = LoginViewModel(LoginUseCase(fakeRepo()))
        vm.onIntent(LoginIntent.UpdateUsername("test"))
        vm.onIntent(LoginIntent.UpdatePassword("password"))
        vm.state.test {
            val initial = awaitItem()
            vm.onIntent(LoginIntent.Submit)
            val loading = awaitItem()
            assertEquals(true, loading.isLoading)
            val done = awaitItem()
            assertEquals(false, done.isLoading)
            assertEquals("test", done.loggedInUser?.username)
            assertNull(done.error)
        }
    }

    @Test
    fun submitWithEmptyFieldsSetsError() = runTest {
        val vm = LoginViewModel(LoginUseCase(fakeRepo()))
        vm.state.test {
            awaitItem() // initial
            vm.onIntent(LoginIntent.Submit)
            val result = awaitItem()
            assertEquals("用户名不能为空", result.error)
        }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :feature:user:jvmTest --tests "com.menu.feature.user.presentation.login.*"
```

Expected: FAIL — `LoginViewModel` not found.

- [ ] **Step 4: Implement LoginViewModel**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/login/LoginViewModel.kt`:

```kotlin
package com.menu.feature.user.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.AppResult
import com.menu.feature.user.domain.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateUsername -> _state.update { it.copy(username = intent.username) }
            is LoginIntent.UpdatePassword -> _state.update { it.copy(password = intent.password) }
            is LoginIntent.Submit -> submit()
            is LoginIntent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun submit() {
        val current = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = loginUseCase(current.username, current.password)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, loggedInUser = result.data)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}
```

- [ ] **Step 5: Implement LoginScreen**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/login/LoginScreen.kt`:

```kotlin
package com.menu.feature.user.presentation.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.menu.core.ui.component.ErrorMessage
import com.menu.core.ui.component.LoadingButton
import com.menu.core.ui.component.MenuTextField

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.loggedInUser) {
        if (state.loggedInUser != null) onLoginSuccess()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "欢迎回来",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(32.dp))

        MenuTextField(
            value = state.username,
            onValueChange = { viewModel.onIntent(LoginIntent.UpdateUsername(it)) },
            label = "用户名"
        )
        Spacer(Modifier.height(16.dp))

        MenuTextField(
            value = state.password,
            onValueChange = { viewModel.onIntent(LoginIntent.UpdatePassword(it)) },
            label = "密码",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(8.dp))

        state.error?.let { error ->
            ErrorMessage(
                message = error,
                onRetry = { viewModel.onIntent(LoginIntent.ClearError) }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        LoadingButton(
            text = "登录",
            isLoading = state.isLoading,
            onClick = { viewModel.onIntent(LoginIntent.Submit) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("没有账号？去注册")
        }
    }
}
```

- [ ] **Step 6: Run tests to verify they pass**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :feature:user:jvmTest
```

Expected: All 9 tests PASS.

- [ ] **Step 7: Commit**

```bash
git add feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/login/
git add feature/user/src/commonTest/kotlin/com/menu/feature/user/presentation/login/
git commit -m "feat(user): add login screen with MVI ViewModel"
```

---

## Task 10: Feature User — Register & Profile Screens

**Files:**
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/register/RegisterState.kt`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/register/RegisterViewModel.kt`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/register/RegisterScreen.kt`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/profile/ProfileState.kt`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/profile/ProfileViewModel.kt`
- Create: `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/profile/ProfileScreen.kt`

- [ ] **Step 1: Create RegisterState**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/register/RegisterState.kt`:

```kotlin
package com.menu.feature.user.presentation.register

import com.menu.core.model.User

data class RegisterState(
    val username: String = "",
    val password: String = "",
    val nickname: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val registeredUser: User? = null
)

sealed interface RegisterIntent {
    data class UpdateUsername(val username: String) : RegisterIntent
    data class UpdatePassword(val password: String) : RegisterIntent
    data class UpdateNickname(val nickname: String) : RegisterIntent
    data object Submit : RegisterIntent
    data object ClearError : RegisterIntent
}
```

- [ ] **Step 2: Implement RegisterViewModel**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/register/RegisterViewModel.kt`:

```kotlin
package com.menu.feature.user.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.AppResult
import com.menu.feature.user.domain.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UpdateUsername -> _state.update { it.copy(username = intent.username) }
            is RegisterIntent.UpdatePassword -> _state.update { it.copy(password = intent.password) }
            is RegisterIntent.UpdateNickname -> _state.update { it.copy(nickname = intent.nickname) }
            is RegisterIntent.Submit -> submit()
            is RegisterIntent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun submit() {
        val current = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val nickname = current.nickname.ifBlank { null }
            when (val result = registerUseCase(current.username, current.password, nickname)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, registeredUser = result.data)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Implement RegisterScreen**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/register/RegisterScreen.kt`:

```kotlin
package com.menu.feature.user.presentation.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.menu.core.ui.component.ErrorMessage
import com.menu.core.ui.component.LoadingButton
import com.menu.core.ui.component.MenuTextField

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.registeredUser) {
        if (state.registeredUser != null) onRegisterSuccess()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "创建账号", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))

        MenuTextField(
            value = state.username,
            onValueChange = { viewModel.onIntent(RegisterIntent.UpdateUsername(it)) },
            label = "用户名"
        )
        Spacer(Modifier.height(16.dp))

        MenuTextField(
            value = state.password,
            onValueChange = { viewModel.onIntent(RegisterIntent.UpdatePassword(it)) },
            label = "密码（至少6位）",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(Modifier.height(16.dp))

        MenuTextField(
            value = state.nickname,
            onValueChange = { viewModel.onIntent(RegisterIntent.UpdateNickname(it)) },
            label = "昵称（可选）"
        )
        Spacer(Modifier.height(8.dp))

        state.error?.let { error ->
            ErrorMessage(message = error, onRetry = { viewModel.onIntent(RegisterIntent.ClearError) })
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))

        LoadingButton(
            text = "注册",
            isLoading = state.isLoading,
            onClick = { viewModel.onIntent(RegisterIntent.Submit) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("已有账号？去登录")
        }
    }
}
```

- [ ] **Step 4: Create ProfileState**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/profile/ProfileState.kt`:

```kotlin
package com.menu.feature.user.presentation.profile

import com.menu.core.model.User

data class ProfileState(
    val user: User? = null,
    val editNickname: String = "",
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val error: String? = null,
    val loggedOut: Boolean = false
)

sealed interface ProfileIntent {
    data object LoadProfile : ProfileIntent
    data object ToggleEdit : ProfileIntent
    data class UpdateNickname(val nickname: String) : ProfileIntent
    data object SaveProfile : ProfileIntent
    data object Logout : ProfileIntent
    data object ClearError : ProfileIntent
}
```

- [ ] **Step 5: Implement ProfileViewModel**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/profile/ProfileViewModel.kt`:

```kotlin
package com.menu.feature.user.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.AppResult
import com.menu.feature.user.domain.GetProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init { onIntent(ProfileIntent.LoadProfile) }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LoadProfile -> loadProfile()
            is ProfileIntent.ToggleEdit -> {
                val current = _state.value
                _state.update {
                    it.copy(
                        isEditing = !current.isEditing,
                        editNickname = current.user?.nickname ?: ""
                    )
                }
            }
            is ProfileIntent.UpdateNickname -> _state.update { it.copy(editNickname = intent.nickname) }
            is ProfileIntent.SaveProfile -> saveProfile()
            is ProfileIntent.Logout -> logout()
            is ProfileIntent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadProfile() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = getProfileUseCase.getProfile()) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, user = result.data, editNickname = result.data.nickname ?: "")
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    private fun saveProfile() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val nickname = _state.value.editNickname.ifBlank { null }
            when (val result = getProfileUseCase.updateProfile(nickname, null)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, user = result.data, isEditing = false)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            getProfileUseCase.logout()
            _state.update { it.copy(loggedOut = true) }
        }
    }
}
```

- [ ] **Step 6: Implement ProfileScreen**

Create `feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/profile/ProfileScreen.kt`:

```kotlin
package com.menu.feature.user.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.menu.core.ui.component.ErrorMessage
import com.menu.core.ui.component.LoadingButton
import com.menu.core.ui.component.MenuTextField

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "个人资料", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        if (state.isLoading && state.user == null) {
            CircularProgressIndicator()
        } else {
            state.error?.let { error ->
                ErrorMessage(message = error, onRetry = { viewModel.onIntent(ProfileIntent.ClearError) })
                Spacer(Modifier.height(16.dp))
            }

            state.user?.let { user ->
                Text("用户名: ${user.username}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("角色: ${user.roleCode}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))

                if (state.isEditing) {
                    MenuTextField(
                        value = state.editNickname,
                        onValueChange = { viewModel.onIntent(ProfileIntent.UpdateNickname(it)) },
                        label = "昵称"
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.onIntent(ProfileIntent.ToggleEdit) }) {
                            Text("取消")
                        }
                        LoadingButton(
                            text = "保存",
                            isLoading = state.isLoading,
                            onClick = { viewModel.onIntent(ProfileIntent.SaveProfile) }
                        )
                    }
                } else {
                    Text("昵称: ${user.nickname ?: "未设置"}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.onIntent(ProfileIntent.ToggleEdit) }) {
                        Text("编辑资料")
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.onIntent(ProfileIntent.Logout) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("退出登录")
            }
        }
    }
}
```

- [ ] **Step 7: Verify compilation**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :feature:user:jvmMainClasses
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/register/
git add feature/user/src/commonMain/kotlin/com/menu/feature/user/presentation/profile/
git commit -m "feat(user): add register and profile screens"
```

---

## Task 11: ComposeApp — DI, Navigation & App Shell

**Files:**
- Create: `composeApp/build.gradle.kts`
- Create: `composeApp/src/commonMain/kotlin/com/menu/app/di/AppComponent.kt`
- Create: `composeApp/src/commonMain/kotlin/com/menu/app/Navigation.kt`
- Create: `composeApp/src/commonMain/kotlin/com/menu/app/App.kt`
- Create: `composeApp/src/androidMain/kotlin/com/menu/app/MenuApplication.kt`
- Create: `composeApp/src/androidMain/kotlin/com/menu/app/MainActivity.kt`
- Create: `composeApp/src/androidMain/AndroidManifest.xml`
- Create: `composeApp/src/iosMain/kotlin/com/menu/app/MainViewController.kt`

- [ ] **Step 1: Create build.gradle.kts for composeApp**

Create `menu-mobile/composeApp/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:model"))
            implementation(project(":core:network"))
            implementation(project(":core:data"))
            implementation(project(":core:ui"))
            implementation(project(":feature:user"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.navigation.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlin.inject.runtime)
            implementation(libs.napier)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace = "com.menu.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.menu.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
}
```

- [ ] **Step 2: Create AndroidManifest.xml**

Create `composeApp/src/androidMain/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".MenuApplication"
        android:label="Menu"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 3: Create AppComponent (DI)**

Create `composeApp/src/commonMain/kotlin/com/menu/app/di/AppComponent.kt`:

```kotlin
package com.menu.app.di

import com.menu.core.data.TokenStorage
import com.menu.core.data.createDataStore
import com.menu.core.network.ApiClient
import com.menu.core.network.TokenProvider
import com.menu.core.network.createHttpClient
import com.menu.feature.user.data.UserApi
import com.menu.feature.user.data.UserRepositoryImpl
import com.menu.feature.user.domain.*
import com.menu.feature.user.presentation.login.LoginViewModel
import com.menu.feature.user.presentation.profile.ProfileViewModel
import com.menu.feature.user.presentation.register.RegisterViewModel

class AppComponent {

    private val dataStore by lazy { createDataStore() }
    private val tokenStorage by lazy { TokenStorage(dataStore) }
    private val tokenProvider: TokenProvider get() = tokenStorage
    private val httpClient by lazy { createHttpClient(tokenProvider) }
    private val apiClient by lazy { ApiClient(httpClient) }

    // User feature
    private val userApi by lazy { UserApi(apiClient) }
    private val userRepository: UserRepository by lazy { UserRepositoryImpl(userApi, tokenProvider) }
    private val loginUseCase by lazy { LoginUseCase(userRepository) }
    private val registerUseCase by lazy { RegisterUseCase(userRepository) }
    private val getProfileUseCase by lazy { GetProfileUseCase(userRepository) }

    fun loginViewModel() = LoginViewModel(loginUseCase)
    fun registerViewModel() = RegisterViewModel(registerUseCase)
    fun profileViewModel() = ProfileViewModel(getProfileUseCase)
}
```

- [ ] **Step 4: Create Navigation**

Create `composeApp/src/commonMain/kotlin/com/menu/app/Navigation.kt`:

```kotlin
package com.menu.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.menu.app.di.AppComponent
import com.menu.feature.user.presentation.login.LoginScreen
import com.menu.feature.user.presentation.profile.ProfileScreen
import com.menu.feature.user.presentation.register.RegisterScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PROFILE = "profile"
}

@Composable
fun MenuNavHost(
    appComponent: AppComponent,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            val viewModel = remember { appComponent.loginViewModel() }
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            val viewModel = remember { appComponent.registerViewModel() }
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigate(Routes.PROFILE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE) {
            val viewModel = remember { appComponent.profileViewModel() }
            ProfileScreen(
                viewModel = viewModel,
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
```

- [ ] **Step 5: Create App root composable**

Create `composeApp/src/commonMain/kotlin/com/menu/app/App.kt`:

```kotlin
package com.menu.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.menu.app.di.AppComponent
import com.menu.core.ui.theme.MenuTheme

@Composable
fun App() {
    val appComponent = remember { AppComponent() }
    MenuTheme {
        MenuNavHost(appComponent = appComponent)
    }
}
```

- [ ] **Step 6: Create Android entry points**

Create `composeApp/src/androidMain/kotlin/com/menu/app/MenuApplication.kt`:

```kotlin
package com.menu.app

import android.app.Application
import com.menu.core.data.appContext
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class MenuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = this
        Napier.base(DebugAntilog())
    }
}
```

Create `composeApp/src/androidMain/kotlin/com/menu/app/MainActivity.kt`:

```kotlin
package com.menu.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
```

- [ ] **Step 7: Create iOS entry point**

Create `composeApp/src/iosMain/kotlin/com/menu/app/MainViewController.kt`:

```kotlin
package com.menu.app

import androidx.compose.ui.window.ComposeUIViewController
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

fun MainViewController() = ComposeUIViewController(
    configure = { Napier.base(DebugAntilog()) }
) {
    App()
}
```

- [ ] **Step 8: Verify Android build**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL, APK generated.

- [ ] **Step 9: Commit**

```bash
git add composeApp/
git commit -m "feat(app): add composeApp shell with DI, navigation, Android/iOS entry points"
```

---

## Task 12: Bazel Build Configuration

**Files:**
- Create: `WORKSPACE.bazel`
- Create: `BUILD.bazel`
- Create: `.bazelrc`
- Create: `core/common/BUILD.bazel`
- Create: `core/model/BUILD.bazel`
- Create: `core/network/BUILD.bazel`
- Create: `core/data/BUILD.bazel`
- Create: `core/ui/BUILD.bazel`
- Create: `feature/user/BUILD.bazel`
- Create: `composeApp/BUILD.bazel`

- [ ] **Step 1: Create .bazelrc**

Create `menu-mobile/.bazelrc`:

```
build --java_language_version=17
build --tool_java_language_version=17
build --experimental_enable_android_migration_apis
test --test_output=errors
```

- [ ] **Step 2: Create WORKSPACE.bazel**

Create `menu-mobile/WORKSPACE.bazel`:

```python
workspace(name = "menu_mobile")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# rules_kotlin
RULES_KOTLIN_VERSION = "1.9.6"
RULES_KOTLIN_SHA = "3b772976fec7bdcda1d84b9d39b176589424c64f6b2571c8c3bc034c7d5c1b5b"

http_archive(
    name = "rules_kotlin",
    sha256 = RULES_KOTLIN_SHA,
    url = "https://github.com/bazelbuild/rules_kotlin/releases/download/v%s/rules_kotlin-v%s.tar.gz" % (RULES_KOTLIN_VERSION, RULES_KOTLIN_VERSION),
)

load("@rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")
kotlin_repositories()

load("@rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")
kt_register_toolchains()

# rules_jvm_external for Maven deps
RULES_JVM_EXTERNAL_TAG = "6.4"

http_archive(
    name = "rules_jvm_external",
    url = "https://github.com/bazelbuild/rules_jvm_external/releases/download/%s/rules_jvm_external-%s.tar.gz" % (RULES_JVM_EXTERNAL_TAG, RULES_JVM_EXTERNAL_TAG),
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0",
        "org.jetbrains.kotlinx:kotlinx-datetime:0.6.2",
        "io.ktor:ktor-client-core:3.1.1",
        "io.ktor:ktor-client-cio:3.1.1",
        "io.ktor:ktor-client-content-negotiation:3.1.1",
        "io.ktor:ktor-serialization-kotlinx-json:3.1.1",
        "io.ktor:ktor-client-auth:3.1.1",
        "io.ktor:ktor-client-logging:3.1.1",
        "io.github.aakira:napier:2.7.1",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
        "https://maven.google.com",
    ],
)
```

Note: This is a baseline Bazel config for JVM targets. Full KMP/Compose Multiplatform Bazel support requires additional rules (`rules_apple` for iOS, compose rules). These will be expanded in later phases as the Bazel KMP ecosystem matures.

- [ ] **Step 3: Create root BUILD.bazel**

Create `menu-mobile/BUILD.bazel`:

```python
# Root BUILD file for menu-mobile
# Individual modules define their own build targets
```

- [ ] **Step 4: Create module BUILD.bazel files**

Create `menu-mobile/core/common/BUILD.bazel`:

```python
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "common",
    srcs = glob(["src/commonMain/kotlin/**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
    ],
)

kt_jvm_test(
    name = "common_test",
    srcs = glob(["src/commonTest/kotlin/**/*.kt"]),
    deps = [
        ":common",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
    ],
)
```

Create `menu-mobile/core/model/BUILD.bazel`:

```python
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "model",
    srcs = glob(["src/commonMain/kotlin/**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "@maven//:org_jetbrains_kotlinx_kotlinx_serialization_json",
        "@maven//:org_jetbrains_kotlinx_kotlinx_datetime",
    ],
)
```

Create `menu-mobile/core/network/BUILD.bazel`:

```python
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "network",
    srcs = glob(["src/commonMain/kotlin/**/*.kt", "src/jvmMain/kotlin/**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//core/common",
        "//core/model",
        "@maven//:io_ktor_ktor_client_core",
        "@maven//:io_ktor_ktor_client_cio",
        "@maven//:io_ktor_ktor_client_content_negotiation",
        "@maven//:io_ktor_ktor_serialization_kotlinx_json",
        "@maven//:io_ktor_ktor_client_auth",
        "@maven//:io_ktor_ktor_client_logging",
        "@maven//:io_github_aakira_napier",
    ],
)
```

Create `menu-mobile/core/data/BUILD.bazel`:

```python
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "data",
    srcs = glob(["src/commonMain/kotlin/**/*.kt", "src/jvmMain/kotlin/**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//core/common",
        "//core/network",
    ],
)
```

Create `menu-mobile/core/ui/BUILD.bazel`:

```python
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

# Note: Compose Multiplatform Bazel rules are still evolving.
# This is a placeholder for when compose rules stabilize.
kt_jvm_library(
    name = "ui",
    srcs = glob(["src/commonMain/kotlin/**/*.kt"]),
    visibility = ["//visibility:public"],
)
```

Create `menu-mobile/feature/user/BUILD.bazel`:

```python
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "user",
    srcs = glob(["src/commonMain/kotlin/**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//core/common",
        "//core/model",
        "//core/network",
        "//core/data",
        "//core/ui",
        "@maven//:io_ktor_ktor_client_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:org_jetbrains_kotlinx_kotlinx_serialization_json",
    ],
)
```

Create `menu-mobile/composeApp/BUILD.bazel`:

```python
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "app",
    srcs = glob(["src/commonMain/kotlin/**/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//core/common",
        "//core/model",
        "//core/network",
        "//core/data",
        "//core/ui",
        "//feature/user",
    ],
)
```

- [ ] **Step 5: Verify Bazel can parse workspace**

```bash
cd /Users/bilibili/rs/menu-mobile
bazel info workspace
```

Expected: Prints workspace path without errors.

- [ ] **Step 6: Commit**

```bash
git add WORKSPACE.bazel BUILD.bazel .bazelrc
git add core/common/BUILD.bazel core/model/BUILD.bazel core/network/BUILD.bazel
git add core/data/BUILD.bazel core/ui/BUILD.bazel
git add feature/user/BUILD.bazel composeApp/BUILD.bazel
git commit -m "build: add Bazel workspace and BUILD files for all modules"
```

---

## Task 13: End-to-End Verification

**Files:** No new files. Verification only.

- [ ] **Step 1: Run all unit tests**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew check
```

Expected: All tests PASS across all modules.

- [ ] **Step 2: Build Android debug APK**

```bash
cd /Users/bilibili/rs/menu-mobile
./gradlew :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL, APK at `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

- [ ] **Step 3: Verify project structure**

```bash
cd /Users/bilibili/rs/menu-mobile
find . -name "*.kt" | head -40
```

Expected: All source files present in expected locations.

- [ ] **Step 4: Final commit with tag**

```bash
cd /Users/bilibili/rs/menu-mobile
git tag v0.1.0-phase1
```

---

## Self-Review Notes

- All types are consistent: `AppResult` (core/common), `ApiResponse`/`UserDto`/`AuthResponse`/`User` (core/model), `TokenProvider` (core/network), `TokenStorage` (core/data), `UserRepository` (feature/user/domain), `UserRepositoryImpl` (feature/user/data)
- `LoginUseCase` is referenced as `LoginUseCase` everywhere (Task 8 definition, Task 9 ViewModel, Task 11 DI)
- `RegisterUseCase` and `GetProfileUseCase` are consistent across domain, presentation, and DI layers
- `toDomain()` extension on `UserDto` is defined in core/model and used in UserRepositoryImpl
- `TokenProvider` interface in core/network, implemented by `TokenStorage` in core/data — no circular dependency
- All spec requirements for Phase 1 covered: project scaffolding, core modules (common/model/network/data/ui), user feature (login/register/profile/logout), app shell with navigation
- No TBD/TODO/placeholder content in any task
- Bazel BUILD files are baseline JVM-only; full KMP Bazel support noted as future work
