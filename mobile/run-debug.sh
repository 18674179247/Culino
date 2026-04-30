#!/bin/bash
# Debug 模式：构建并安装到连接的设备
set -e

cd "$(dirname "$0")"

echo "==> 设置 ADB 端口转发..."
adb reverse tcp:3000 tcp:3000
adb reverse tcp:9000 tcp:9000

echo "==> 构建 Debug APK..."
./gradlew :composeApp:installDebug

echo "==> 启动应用..."
adb shell am start -n com.culino.app/.MainActivity

echo "==> Done. API -> http://127.0.0.1:3000/api/v1/"
