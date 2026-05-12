#!/usr/bin/env bash
# ==========================================================================
# Culino 生产部署辅助脚本
#
# 用法:
#   ./deploy/prod.sh init        # 首次部署：初始化目录、检查 .env
#   ./deploy/prod.sh up          # 启动所有服务
#   ./deploy/prod.sh down        # 停止所有服务
#   ./deploy/prod.sh restart     # 重启
#   ./deploy/prod.sh logs [svc]  # 查看日志
#   ./deploy/prod.sh ps          # 查看容器状态
#   ./deploy/prod.sh backup      # 备份数据库
#   ./deploy/prod.sh update      # 拉取最新镜像并重启 app
# ==========================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$BACKEND_DIR"

COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env.prod"
DC="docker compose -f $COMPOSE_FILE --env-file $ENV_FILE"

check_env() {
    if [[ ! -f "$ENV_FILE" ]]; then
        echo "错误: 未找到 $ENV_FILE"
        echo "请执行: cp .env.prod.example $ENV_FILE 并填入生产配置"
        exit 1
    fi
    if grep -Eq '^(POSTGRES_PASSWORD|REDIS_PASSWORD|JWT_SECRET|S3_ACCESS_KEY|S3_SECRET_KEY|DEEPSEEK_API_KEY)=REPLACE_WITH' "$ENV_FILE"; then
        echo "错误: $ENV_FILE 中仍存在未替换的占位符（REPLACE_WITH_*）"
        grep -En '=REPLACE_WITH' "$ENV_FILE" || true
        exit 1
    fi
}

cmd_init() {
    check_env
    # 读取 DATA_ROOT
    DATA_ROOT=$(grep -E '^DATA_ROOT=' "$ENV_FILE" | tail -n1 | cut -d= -f2- || true)
    DATA_ROOT="${DATA_ROOT:-/data/culino}"

    echo "==> 初始化数据目录: $DATA_ROOT"
    for d in pgdata redisdata rustfsdata logs; do
        if [[ ! -d "$DATA_ROOT/$d" ]]; then
            sudo mkdir -p "$DATA_ROOT/$d"
        fi
    done
    sudo chown -R "$(id -u):$(id -g)" "$DATA_ROOT"
    echo "==> 数据目录就绪"

    echo "==> 校验 compose 配置"
    $DC config >/dev/null
    echo "==> 初始化完成，运行: ./deploy/prod.sh up"
}

cmd_up() {
    check_env
    $DC up -d
    $DC ps
}

cmd_down() {
    $DC down
}

cmd_restart() {
    check_env
    $DC restart "${1:-}"
}

cmd_logs() {
    $DC logs -f --tail 200 "${1:-app}"
}

cmd_ps() {
    $DC ps
}

cmd_backup() {
    check_env
    DATA_ROOT=$(grep -E '^DATA_ROOT=' "$ENV_FILE" | tail -n1 | cut -d= -f2-)
    DATA_ROOT="${DATA_ROOT:-/data/culino}"
    BACKUP_DIR="$DATA_ROOT/backups"
    mkdir -p "$BACKUP_DIR"

    STAMP=$(date +%Y%m%d_%H%M%S)
    POSTGRES_USER=$(grep -E '^POSTGRES_USER=' "$ENV_FILE" | tail -n1 | cut -d= -f2-)
    POSTGRES_DB=$(grep -E '^POSTGRES_DB=' "$ENV_FILE" | tail -n1 | cut -d= -f2-)
    POSTGRES_USER="${POSTGRES_USER:-culino}"
    POSTGRES_DB="${POSTGRES_DB:-culino_db}"

    echo "==> 备份 PostgreSQL 到 $BACKUP_DIR/pg_${STAMP}.sql.gz"
    $DC exec -T db pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" | gzip > "$BACKUP_DIR/pg_${STAMP}.sql.gz"
    echo "==> 清理 14 天前的备份"
    find "$BACKUP_DIR" -name 'pg_*.sql.gz' -mtime +14 -delete
    ls -lh "$BACKUP_DIR" | tail -5
}

cmd_update() {
    check_env
    echo "==> 拉取最新镜像"
    $DC pull app
    echo "==> 重启 app 容器"
    $DC up -d app
    $DC ps app
}

case "${1:-}" in
    init)    cmd_init ;;
    up)      cmd_up ;;
    down)    cmd_down ;;
    restart) cmd_restart "${2:-}" ;;
    logs)    cmd_logs "${2:-app}" ;;
    ps)      cmd_ps ;;
    backup)  cmd_backup ;;
    update)  cmd_update ;;
    *)
        echo "用法: $0 {init|up|down|restart|logs|ps|backup|update}"
        exit 1
        ;;
esac
