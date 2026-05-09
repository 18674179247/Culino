# Debug 调试指南

## 构建模式说明

项目配置了两种构建模式：

| 模式 | API 地址 | 用途 |
|------|---------|------|
| debug | `http://10.0.2.2:3000/api/v1/` | 本地开发调试，通过 ADB 端口转发连接宿主机 |
| release | `https://your-prod-domain.com/api/v1/` | 生产环境 |

`10.0.2.2` 是 Android 模拟器中访问宿主机 `127.0.0.1` 的特殊地址。

## Debug 模式测试步骤

### 1. 启动后端服务

```bash
cd backend
docker compose up -d     # 启动 PostgreSQL、Redis、RustFS
cargo run -- serve        # 启动后端，默认监听 0.0.0.0:3000
```

确认后端正常运行：

```bash
curl http://127.0.0.1:3000/api/v1/user/health
```

### 2. 使用模拟器测试

直接运行 debug 构建即可，模拟器会通过 `10.0.2.2` 自动访问宿主机的 3000 端口：

```bash
cd culino-mobile
./gradlew :composeApp:installDebug
```

或在 Android Studio 中选择 debug 变体后点击 Run。

### 3. 使用真机测试（ADB 端口转发）

真机无法直接访问 `10.0.2.2`，需要通过 ADB 端口转发：

```bash
# 将手机的 3000 端口转发到电脑的 3000 端口
adb reverse tcp:3000 tcp:3000
adb reverse tcp:9000 tcp:9000
```

转发后，手机上访问 `10.0.2.2:3000` 的请求会被路由到电脑的 `3000` 端口。

验证转发是否生效：

```bash
adb reverse --list
```

如果需要移除转发：

```bash
adb reverse --remove tcp:3000
```

### 4. 构建 Debug APK

```bash
./gradlew :composeApp:assembleDebug
```

APK 输出路径：`composeApp/build/outputs/apk/debug/composeApp-debug.apk`

### 5. 构建 Release APK

```bash
./gradlew :composeApp:assembleRelease
```

## 网络配置原理

- `composeApp/build.gradle.kts` 中通过 `buildConfigField` 为 debug 和 release 注入不同的 `API_BASE_URL`
- `CulinoApplication.onCreate()` 在应用启动时将 `BuildConfig.API_BASE_URL` 写入 `Constants.API_BASE_URL`
- `HttpClientFactory` 使用 `Constants.API_BASE_URL` 作为所有请求的基础地址

## 常见问题

### 模拟器无法连接后端

1. 确认后端监听地址是 `0.0.0.0:3000` 而不是 `127.0.0.1:3000`
2. 检查防火墙是否放行了 3000 端口

### 真机 ADB 转发不生效

1. 确认 USB 调试已开启
2. 重新执行 `adb reverse tcp:3000 tcp:3000`
3. 如果使用无线调试，确保 ADB 已连接：`adb devices`

### 切换到 Release 模式

修改 `composeApp/build.gradle.kts` 中 release 的 `API_BASE_URL` 为实际的生产域名，然后构建 release 包。
