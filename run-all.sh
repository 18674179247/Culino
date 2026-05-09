#!/bin/bash
# 一键重启后端 + 构建运行前端 Debug
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"

# 停止旧的后端进程
echo "==> 停止旧后端进程..."
pkill -f "culino-backend" 2>/dev/null || true
sleep 1

# 启动后端
echo "==> 编译并启动后端..."
cd "$ROOT/backend"
cargo build 2>&1 | tail -3
./target/debug/culino-backend &
BACKEND_PID=$!
echo "    后端 PID: $BACKEND_PID"

# 等待后端就绪
echo "==> 等待后端启动..."
for i in $(seq 1 15); do
    if curl -s http://127.0.0.1:3000/api/v1/recipe/random > /dev/null 2>&1; then
        echo "    后端已就绪"
        break
    fi
    sleep 1
done

# 运行前端
echo "==> 构建并安装前端 Debug..."
cd "$ROOT/frontend"
bash .run/android-debug.sh

echo "==> 全部完成！后端 PID: $BACKEND_PID"
