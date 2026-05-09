#!/bin/bash
# iOS Debug 模式：构建并安装到连接的 iPhone 真机，支持连调
set -e

cd "$(dirname "$0")/.."

# --- 配置 ---
PROJECT_DIR="app/iosApp"
PROJECT_NAME="iosApp"
SCHEME="iosApp"
BUNDLE_ID="com.culino.app"
CONFIGURATION="Debug"

# --- 检查设备连接 ---
echo "==> 检查已连接的 iOS 设备..."

DEVICE_ID=$(xcrun xctrace list devices 2>&1 | grep -v "Simulator" | grep -v "^==" | grep -v "^$" | grep -v "Mac" | grep -v "This computer" | awk -F'[()]' '{for(i=1;i<=NF;i++){if($i~/^[0-9a-fA-F-]{20,}$/){print $i}}}' | head -1)

if [ -z "$DEVICE_ID" ]; then
    echo "错误：未检测到已连接的 iOS 设备"
    echo ""
    echo "请确保："
    echo "  1. iPhone 通过 USB 连接到 Mac"
    echo "  2. 已在 iPhone 上信任此电脑"
    echo "  3. 开发者模式已开启（设置 > 隐私与安全性 > 开发者模式）"
    echo ""
    echo "可用设备列表："
    xcrun xctrace list devices 2>&1 | grep -v "Simulator"
    exit 1
fi

echo "    设备 ID: $DEVICE_ID"

# --- 检查 Team ID ---
TEAM_ID=$(grep "DEVELOPMENT_TEAM" "$PROJECT_DIR/$PROJECT_NAME.xcodeproj/project.pbxproj" | grep -v '""' | head -1 | sed 's/.*= *"\(.*\)".*/\1/' || true)

if [ -z "$TEAM_ID" ]; then
    echo ""
    echo "错误：未配置 Development Team"
    echo ""
    echo "首次使用需要在 Xcode 中配置签名（只需一次）："
    echo "  1. 打开 app/iosApp/iosApp.xcodeproj"
    echo "  2. 选择 iosApp target > Signing & Capabilities"
    echo "  3. 勾选 Automatically manage signing"
    echo "  4. Team 选择你的 Apple ID（Personal Team）"
    echo "  5. 如果 Bundle ID 冲突，改为唯一的 ID（如 com.culino.app.你的名字）"
    echo ""
    echo "配置完成后重新运行此脚本即可。"
    echo ""
    echo "正在打开 Xcode..."
    open "$PROJECT_DIR/$PROJECT_NAME.xcodeproj"
    exit 1
fi

echo "    Team ID: $TEAM_ID"

# --- 获取 Mac 本机 IP（用于连调提示）---
MAC_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "未知")

# --- 构建 ---
echo ""
echo "==> 构建 iOS Debug (真机)..."
xcodebuild \
    -project "$PROJECT_DIR/$PROJECT_NAME.xcodeproj" \
    -scheme "$SCHEME" \
    -configuration "$CONFIGURATION" \
    -destination "id=$DEVICE_ID" \
    -allowProvisioningUpdates \
    -derivedDataPath build/ios-debug \
    build \
    2>&1 | tail -5

# --- 安装并启动 ---
echo ""
echo "==> 安装到设备..."
APP_PATH=$(find build/ios-debug -name "*.app" -path "*Debug-iphoneos*" | head -1)

if [ -z "$APP_PATH" ]; then
    echo "错误：未找到构建产物"
    exit 1
fi

# 使用 ios-deploy 安装（如果可用），否则用 xcodebuild
if command -v ios-deploy &> /dev/null; then
    echo "==> 使用 ios-deploy 安装并启动..."
    ios-deploy --bundle "$APP_PATH" --debug --noninteractive 2>&1 | tail -3 || \
    ios-deploy --bundle "$APP_PATH" --justlaunch
else
    echo "==> 使用 devicectl 安装并启动..."
    # macOS 14+ / Xcode 15+ 使用 devicectl
    if command -v xcrun &> /dev/null && xcrun devicectl list devices &> /dev/null; then
        xcrun devicectl device install app --device "$DEVICE_ID" "$APP_PATH" 2>&1 | tail -3
        xcrun devicectl device process launch --device "$DEVICE_ID" "$BUNDLE_ID" 2>&1 | tail -3
    else
        echo ""
        echo "App 已构建成功，但自动安装需要 ios-deploy 工具。"
        echo "安装方式：brew install ios-deploy"
        echo ""
        echo "或者手动操作：在 Xcode 中按 Cmd+R 运行到设备。"
        open "$PROJECT_DIR/$PROJECT_NAME.xcodeproj"
        exit 0
    fi
fi

# --- 完成 ---
echo ""
echo "==> Done!"
echo ""
echo "连调说明："
echo "  iOS 设备通过 USB 连接时，可直接访问 Mac 的网络服务。"
echo "  当前 API 地址: http://127.0.0.1:3000/api/v1/"
echo ""
echo "  注意：iOS 真机不能直接访问 127.0.0.1（那是手机自己）。"
echo "  如需连调本地后端，请将 API 地址改为 Mac 的局域网 IP："
echo "  Mac IP: http://$MAC_IP:3000/api/v1/"
echo ""
echo "  修改位置: app/src/iosMain/kotlin/com/culino/app/MainViewController.kt"
