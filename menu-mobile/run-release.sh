#!/bin/bash
# Release 模式：构建 Release APK
set -e

cd "$(dirname "$0")"

echo "==> 构建 Release APK..."
./gradlew :composeApp:assembleRelease

APK_PATH="composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk"
echo "==> APK 输出: $APK_PATH"
