# Debug 调试指南

## 构建模式

| 模式 | API 地址 | 用途 |
|------|---------|------|
| debug | `http://10.0.2.2:3000/api/v1/` | 本地开发，模拟器通过 ADB 端口转发连接宿主机 |
| release | 生产域名 | 生产环境 |

`10.0.2.2` 是 Android 模拟器访问宿主机 `127.0.0.1` 的特殊地址。

## 测试步骤

### 1. 启动后端

```bash
cd backend
docker compose up -d db redis rustfs
cargo run
# 确认: curl http://127.0.0.1:3000/api/v1/user/health
```

### 2. 模拟器测试

模拟器自动通过 `10.0.2.2` 访问宿主机 3000 端口：

```bash
cd frontend
./gradlew :app:installDebug
```

### 3. 真机测试（ADB 端口转发）

```bash
adb reverse tcp:3000 tcp:3000
adb reverse tcp:9000 tcp:9000   # 对象存储
adb reverse --list              # 验证
```

### 4. Web 测试

```bash
cd frontend
./gradlew :app:wasmJsBrowserDevelopmentRun
```

Web 端通过 `window.location` 推导 API 地址。

## 网络配置原理

- `Constants.API_BASE_URL` 在各平台入口启动时设置
  - Android: `BuildConfig.API_BASE_URL`
  - iOS: `Info.plist`
  - Web: `window.location` 推导
- `HttpClientFactory` 使用 `Constants.API_BASE_URL` 作为所有请求的基础地址

## 常见问题

| 问题 | 解决方案 |
|------|---------|
| 模拟器无法连接后端 | 确认后端监听 `0.0.0.0:3000`，检查防火墙 |
| 真机 ADB 转发不生效 | 确认 USB 调试开启，重新执行 `adb reverse` |
| Web CORS 错误 | 确认后端 CORS 配置允许 localhost |
