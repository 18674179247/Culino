#!/bin/bash
# Release 模式：构建 Release APK
set -e

cd "$(dirname "$0")/.."

echo "==> 构建 Release APK..."
./gradlew :app:assembleRelease

APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
echo "==> APK 输出: $APK_PATH"
