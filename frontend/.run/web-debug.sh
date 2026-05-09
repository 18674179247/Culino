#!/bin/bash
# Web Debug 模式:启动 wasmJs 开发服务器,浏览器访问 http://localhost:8080
#
# 前置条件:
#   1. 后端已启动(默认 http://127.0.0.1:3000)
#   2. 后端 .env 的 CORS_ORIGINS 需包含 http://localhost:8080
set -e

cd "$(dirname "$0")/.."

PORT="${WEB_DEV_PORT:-8080}"

echo "==> 启动 wasmJs 开发服务器(端口 $PORT)..."
echo "    访问地址: http://localhost:$PORT"
echo "    API 目标: http://127.0.0.1:3000/api/v1/"
echo ""
echo "    首次编译会下载 Skiko/Kotlin Native 依赖,耗时较久。"
echo "    修改代码后 webpack-dev-server 会自动热更新。"
echo ""
echo "    停止服务:Ctrl+C"
echo ""

if [ "$PORT" != "8080" ]; then
    ./gradlew :app:wasmJsBrowserDevelopmentRun \
        -Pwebpack.devServer.port=$PORT
else
    ./gradlew :app:wasmJsBrowserDevelopmentRun
fi
