# Culino 单服务器部署指南

> 把 Rust 后端 + Web 前端（wasmJs）+ 依赖组件（Postgres / Redis / RustFS）部署到同一台 Linux 服务器的完整方案。
> Android / iOS 是原生客户端，不部署到服务器；本文只覆盖 Web 端。

---

## 1. 整体架构

```
                    ┌──────────────────────────────────────┐
                    │           公网用户 / APP              │
                    └───────────────┬──────────────────────┘
                                    │ HTTPS (443)
                                    ▼
                        ┌───────────────────────┐
                        │        Nginx          │  ← 入口，TLS 卸载
                        │  (宿主机或容器)        │
                        └──────┬─────────┬──────┘
                  静态资源      │         │   反向代理
                  /  →         │         │   /api  → backend:3000
                               ▼         ▼
                    ┌──────────────┐  ┌─────────────────────┐
                    │  Web 前端    │  │  Rust Backend 容器   │
                    │ (静态文件)    │  │  (culino-backend)    │
                    └──────────────┘  └──────┬──────────────┘
                                             │ docker 内网
                              ┌──────────────┼──────────────┐
                              ▼              ▼              ▼
                         ┌─────────┐    ┌─────────┐   ┌──────────┐
                         │Postgres │    │ Redis   │   │ RustFS   │
                         │  :5432  │    │  :6379  │   │  :9000   │
                         └─────────┘    └─────────┘   └──────────┘
                              ▲
                              │ 卷挂载
                         /data/pgdata (宿主机)
```

**关键决策**：
- 只暴露 443/80（Nginx），其它端口全部走 Docker 内网，**不映射到宿主机**
- 数据库和对象存储用**具名卷（named volume）或绑定挂载到宿主机目录**，方便备份
- 前端作为**静态资源**由 Nginx 直接托管，不走后端

---

## 2. 服务器最低配置建议

| 项目 | 推荐 | 说明 |
|------|------|------|
| CPU | 2 核 | Rust 运行时占用低；但**在服务器打包**时编译阶段需要 4 核更舒服 |
| 内存 | 4 GB 起步 | Postgres(~300M) + Redis(~100M) + RustFS(~200M) + Backend(~150M) + 余量给编译/缓存 |
| 磁盘 | 40 GB+ | 数据库 + 图片对象存储会持续增长；图片建议单独挂盘 |
| 系统 | Ubuntu 22.04 / Debian 12 | 与 Dockerfile 的 `debian:bookworm-slim` 一致，行为最可预期 |
| 架构 | x86_64 (amd64) 优先 | ARM 也能跑，但涉及跨架构交叉编译，后文说明 |

> 内存 < 2GB 的机器跑 `cargo build --release` 容易 OOM，这时必须在本地/CI 打包。

---

## 3. 打包策略：本地打包 vs 服务器打包

这是整个部署里最关键的一个决策。三种方案对比：

### 方案 A：服务器上直接 `docker compose up --build`（最简单）

**优点**：流程简单，`git pull && docker compose up -d --build` 一键搞定，没有跨平台问题（容器内都是 linux）。

**缺点与坑**：
- **依赖下载慢 / 失败**：Rust 首次编译会拉几百个 crate，国内服务器容易超时。Node、Gradle 同理。
- **编译资源消耗大**：Rust release 编译 CPU 和内存占用很高，2 核 2G 机器会卡到连 SSH 都卡。
- **构建缓存丢失**：每次 `docker compose build` 如果没命中缓存，会重新编译所有依赖（你当前的 Dockerfile 已经做了 stub 分层，能部分缓解）。

**缓解措施**（用这个方案时必做）：

1. **配置 Cargo 国内镜像**（在 Dockerfile builder 阶段加入）：
   ```dockerfile
   RUN mkdir -p ~/.cargo && cat > ~/.cargo/config.toml <<EOF
   [source.crates-io]
   replace-with = 'rsproxy-sparse'
   [source.rsproxy-sparse]
   registry = "sparse+https://rsproxy.cn/index/"
   [registries.rsproxy]
   index = "https://rsproxy.cn/crates.io-index"
   [net]
   git-fetch-with-cli = true
   EOF
   ```
2. **Docker 镜像加速**：`/etc/docker/daemon.json` 配置 `registry-mirrors`（阿里云、DaoCloud 等）
3. **分配 swap**：低配机器务必开 4~8G swap，防止编译 OOM
   ```bash
   sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile
   sudo mkswap /swapfile && sudo swapon /swapfile
   ```
4. **构建产物持久化**：Docker BuildKit 缓存 + volume 缓存 target 目录（可选）

### 方案 B：本地/CI 打包 Docker 镜像，推送到 Registry（推荐生产用）

**流程**：
```
本地 docker build → push 到 registry → 服务器 docker pull → docker compose up
```

**优点**：
- 服务器只负责运行，CPU/内存压力小
- 构建环境可控，CI 可复现
- 部署快（只拉镜像，不编译）

**关键点（避坑）**：

- **必须构建 linux/amd64 架构**（除非服务器是 ARM）。Mac M 系列本地默认是 `linux/arm64`，直接推镜像到 x86 服务器会报 `exec format error`。
  ```bash
  # Mac 上构建 x86 镜像
  docker buildx build --platform linux/amd64 -t your-registry/culino-backend:v1 --push .
  ```
- **Registry 选择**：公网用 Docker Hub / GHCR / 阿里云容器镜像服务；内网用 Harbor。
- **敏感信息不要打进镜像**：`JWT_SECRET`、`DEEPSEEK_API_KEY` 必须走环境变量或 secrets，不能 `COPY .env` 进镜像。

### 方案 C：本地 Gradle 构建前端静态产物，服务器只跑后端

**适用场景**：前端改动频繁、不想每次部署都跑 Gradle（wasmJs 构建比较重）。

**流程**：
```bash
# 本地
cd frontend
./gradlew :app:wasmJsBrowserDistribution
# 产物在 frontend/app/build/dist/wasmJs/productionExecutable/

# 传到服务器
rsync -avz --delete frontend/app/build/dist/wasmJs/productionExecutable/ \
  user@server:/var/www/culino-web/
```

**这个方案不存在"Mac 包在 Linux 不可用"的问题**，因为 wasmJs 产物是 `.wasm` + `.js` + 静态资源，跨平台。Android/iOS 构建产物也是 apk/ipa，跟服务器无关。

**会有 Mac/Win 构建产物在 Linux 不可用的情况**，只发生在：
- 后端 Rust 在 Mac 上 `cargo build` 出的二进制不能直接放到 Linux（动态链接、平台 ABI 不同）→ 必须用 Docker 构建或交叉编译
- Node native 模块（含 C++ 绑定，如旧版 `node-sass`）→ **本项目不涉及**

### 推荐组合

- **个人小规模**：方案 A（服务器上 docker compose build），加镜像加速和 swap
- **正式生产**：方案 B（本地/CI 打后端镜像）+ 方案 C（本地打 Web 静态产物）

---

## 4. Docker 配置优化

### 4.1 修正当前 `docker-compose.yml` 的生产级问题

现有配置主要面向开发。生产部署前必须改动：

```yaml
# docker-compose.prod.yml
services:
  db:
    image: postgres:16
    restart: always              # ← 新增：崩溃自动重启
    environment:
      POSTGRES_USER: culino
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}   # ← 从 .env 读,不要硬编码
      POSTGRES_DB: culino_db
    # ports: 不要暴露 5432 到公网！   ← 删掉 ports 映射
    volumes:
      - /data/culino/pgdata:/var/lib/postgresql/data   # ← 绑定到宿主机目录,方便备份
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U culino"]
      interval: 10s
      retries: 5
    networks: [culino-net]

  redis:
    image: redis:7-alpine
    restart: always
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    # ports: 同样删掉,容器间走内网即可
    volumes:
      - /data/culino/redisdata:/data
    networks: [culino-net]

  rustfs:
    image: rustfs/rustfs:latest
    restart: always
    command: server /data --console-address ":9001"
    environment:
      RUSTFS_ROOT_USER: ${S3_ACCESS_KEY}
      RUSTFS_ROOT_PASSWORD: ${S3_SECRET_KEY}
    # 只暴露控制台到本机,不要公网
    ports:
      - "127.0.0.1:9001:9001"
    volumes:
      - /data/culino/rustfsdata:/data
    networks: [culino-net]

  app:
    image: your-registry/culino-backend:${IMAGE_TAG:-latest}   # 方案B:拉镜像
    # 或 build: . (方案A)
    restart: always
    ports:
      - "127.0.0.1:3000:3000"    # ← 只监听本机,由 Nginx 代理
    environment:
      DATABASE_URL: postgres://culino:${POSTGRES_PASSWORD}@db:5432/culino_db
      REDIS_URL: redis://:${REDIS_PASSWORD}@redis:6379
      RUN_MODE: production
      JWT_SECRET: ${JWT_SECRET:?JWT_SECRET must be set}
      DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY:?}
      S3_ENDPOINT: http://rustfs:9000
      S3_ACCESS_KEY: ${S3_ACCESS_KEY}
      S3_SECRET_KEY: ${S3_SECRET_KEY}
      S3_BUCKET: culino
      SERVER_ADDR: 0.0.0.0:3000
      LOG_LEVEL: info
      LOG_DIR: /app/logs
      CORS_ORIGINS: https://your-domain.com     # ← 生产必须配具体域名
    volumes:
      - /data/culino/logs:/app/logs
    depends_on:
      db: { condition: service_healthy }
      redis: { condition: service_started }
      rustfs: { condition: service_started }
    networks: [culino-net]

networks:
  culino-net:
    driver: bridge
```

配套 `.env`（**不要提交 git**）：
```bash
POSTGRES_PASSWORD=$(openssl rand -base64 24)
REDIS_PASSWORD=$(openssl rand -base64 24)
JWT_SECRET=$(openssl rand -base64 48)
S3_ACCESS_KEY=$(openssl rand -hex 16)
S3_SECRET_KEY=$(openssl rand -base64 32)
DEEPSEEK_API_KEY=sk-xxx
IMAGE_TAG=v1.0.0
```

### 4.2 主要修改点说明

| 改动 | 原因 |
|------|------|
| 删除 db/redis 的 `ports` 映射 | 原配置把 5432/6379 暴露到公网，会被扫描爆破 |
| `127.0.0.1:3000:3000` | 后端只监听本机，强制走 Nginx |
| `restart: always` | 机器重启或进程崩溃自动拉起 |
| `healthcheck` + `depends_on.condition` | 避免 app 在 db 没 ready 时启动失败 |
| 绑定挂载到 `/data/culino/*` | 数据在宿主机可见，备份和迁移方便 |
| 密码全部走 `.env` | 不硬编码到 compose / 镜像 |
| `CORS_ORIGINS` 设具体域名 | 开发的 `localhost:8080` 不能上生产 |

---

## 5. Nginx 配置

### 5.1 安装

宿主机直接装（简单，推荐）：
```bash
sudo apt install nginx certbot python3-certbot-nginx
```

### 5.2 配置文件 `/etc/nginx/sites-available/culino.conf`

```nginx
# HTTP → HTTPS 跳转
server {
    listen 80;
    server_name your-domain.com;
    location /.well-known/acme-challenge/ { root /var/www/certbot; }
    location / { return 301 https://$host$request_uri; }
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_session_cache shared:SSL:10m;

    # 安全头
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header Referer-Policy "strict-origin-when-cross-origin" always;

    # 请求体上限(菜谱图片上传,调大一点)
    client_max_body_size 20M;

    # ---------- Web 前端静态资源 ----------
    root /var/www/culino-web;
    index index.html;

    # wasm 必须带正确 MIME,否则浏览器拒绝执行
    location ~ \.wasm$ {
        default_type application/wasm;
        add_header Cache-Control "public, max-age=31536000, immutable";
    }

    # JS/CSS/图片长缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|svg|woff2?)$ {
        add_header Cache-Control "public, max-age=31536000, immutable";
    }

    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # ---------- 后端 API 反向代理 ----------
    location /api/ {
        proxy_pass http://127.0.0.1:3000/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For  $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # AI 请求可能较慢(DeepSeek),放宽超时
        proxy_read_timeout 120s;
        proxy_send_timeout 120s;
    }

    # Swagger UI（生产建议加 basic auth 或直接关闭）
    location /swagger-ui/ {
        # auth_basic "restricted";
        # auth_basic_user_file /etc/nginx/.htpasswd;
        proxy_pass http://127.0.0.1:3000/swagger-ui/;
    }
}
```

### 5.3 启用 + HTTPS 证书

```bash
sudo ln -s /etc/nginx/sites-available/culino.conf /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

# 申请 Let's Encrypt 证书(自动改 nginx 配置)
sudo certbot --nginx -d your-domain.com

# certbot 会自动加 cron 续期,验证:
sudo systemctl status certbot.timer
```

---

## 6. 公网 IP、域名、HTTPS

### 6.1 公网 IP

- **有公网 IP 的云服务器**（阿里云 / 腾讯云 / AWS）：直接用即可，记得在云控制台安全组**只开 22/80/443**
- **家庭宽带**：大部分运营商给的是 NAT 后的内网 IP，无法直接建站。可选：
  - 申请公网 IPv4（部分运营商需企业/额外付费）
  - 用 frp / Cloudflare Tunnel / ngrok 打穿
  - 用 IPv6（国内覆盖已不错，但用户侧也需要 IPv6）
- **国内部署**：**域名必须备案**（ICP 备案），否则 80/443 会被运营商拦截。不想备案可选香港 / 海外机房。

### 6.2 域名

- DNS A 记录指向服务器公网 IP
- 建议一级域名+子域名分离：`api.xxx.com` 走后端，`app.xxx.com` 走前端，或统一 `xxx.com` 用路径区分（本文档采用后者）
- TTL 先设 600s，稳定后调到 3600s

### 6.3 HTTPS

- **免费证书**：Let's Encrypt（certbot）/ ZeroSSL，90 天自动续期
- **通配符证书**：需要 DNS-01 验证，申请方式：`certbot certonly --manual --preferred-challenges dns -d '*.xxx.com'`
- **国内 CDN 托管**（阿里云 / 腾讯云 / Cloudflare）：证书由 CDN 管理，源站可以不装证书，但要限制源站只接受 CDN 回源 IP

---

## 7. 安全清单

### 7.1 操作系统层

```bash
# 创建非 root 部署账户
sudo adduser deploy && sudo usermod -aG docker,sudo deploy

# 禁用 root SSH 登录 + 改用密钥
sudo vi /etc/ssh/sshd_config
  # PermitRootLogin no
  # PasswordAuthentication no
sudo systemctl restart sshd

# 防火墙(ufw)
sudo ufw default deny incoming
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# 防爆破(fail2ban)
sudo apt install fail2ban
```

### 7.2 应用层

| 项目 | 动作 |
|------|------|
| `JWT_SECRET` | 生产必须随机（`openssl rand -base64 48`），**绝不能**用 `change-me-in-production` |
| `DEEPSEEK_API_KEY` | 走环境变量，不要提交 git，定期轮换 |
| DB/Redis 密码 | 同样走 `.env`，且不映射端口到公网 |
| RustFS 凭证 | 不要用默认 `minioadmin/minioadmin` |
| CORS | `CORS_ORIGINS` 只允许正式域名，不要配 `*` |
| Swagger UI | 生产加 basic auth 或按 feature flag 关闭 |
| 日志 | `LOG_LEVEL=info`，避免 debug 泄露请求体 |
| 速率限制 | 项目已有 `RATE_LIMITING.md`，确认生产启用 |

### 7.3 Docker 层

- 不要用 `:latest` tag，锁定版本（`postgres:16.4`、`redis:7.2-alpine`）
- 容器非 root 运行（在 Dockerfile 最后加 `USER nonroot` 或 compose 里 `user: "1000:1000"`）
- `read_only: true` + `tmpfs` 给无状态服务（app 容器若不写本地文件可以加）
- 定期 `docker system prune` 清理悬空镜像

### 7.4 数据备份

```bash
# 每日备份 Postgres
0 3 * * * docker exec culino-db-1 pg_dump -U culino culino_db | gzip > /backup/pg_$(date +\%F).sql.gz

# 保留 14 天
find /backup -name 'pg_*.sql.gz' -mtime +14 -delete

# RustFS 数据目录直接 rsync 到备份盘或异地
```

---

## 8. 部署流程（首次）

```bash
# 1. 服务器准备
ssh deploy@server
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker deploy && newgrp docker

# 2. 拉代码 / 上传镜像
cd ~ && git clone <repo> culino && cd culino/backend

# 3. 配置
cp .env.example .env
vi .env                  # 改所有密钥
cp docker-compose.yml docker-compose.prod.yml
vi docker-compose.prod.yml   # 按第 4.1 节修改

# 4. 创建数据目录
sudo mkdir -p /data/culino/{pgdata,redisdata,rustfsdata,logs}
sudo chown -R deploy:deploy /data/culino

# 5. 构建 web 前端（本地做,上传产物）
# 本地:
cd frontend && ./gradlew :app:wasmJsBrowserDistribution
rsync -avz app/build/dist/wasmJs/productionExecutable/ \
  deploy@server:/var/www/culino-web/

# 6. 启动后端
cd ~/culino/backend
docker compose -f docker-compose.prod.yml --env-file .env up -d

# 7. 检查
docker compose ps
docker compose logs -f app
curl http://127.0.0.1:3000/api/health   # 替换为你的健康检查端点

# 8. 配置 Nginx + HTTPS（见第 5 节）

# 9. 初始化 RustFS bucket(若后端不自动建)
docker exec -it culino-rustfs-1 sh
  mc alias set local http://localhost:9000 $S3_ACCESS_KEY $S3_SECRET_KEY
  mc mb local/culino
```

## 9. 日常运维

```bash
# 更新后端（方案 B）
docker compose pull app
docker compose up -d app

# 更新前端
rsync -avz --delete <本地产物>/ server:/var/www/culino-web/

# 查看日志
docker compose logs -f app --tail 200
tail -f /data/culino/logs/*.log

# 数据库交互
docker exec -it culino-db-1 psql -U culino -d culino_db

# 回滚
docker compose down
docker image tag your-registry/culino-backend:v0.9.0 your-registry/culino-backend:latest
docker compose up -d
```

---

## 10. 常见坑 & 排查

| 现象 | 原因 | 解决 |
|------|------|------|
| `exec format error` | 镜像架构不匹配（Mac M 系列默认 arm64） | `docker buildx build --platform linux/amd64` |
| 后端启动报 DB 连接失败 | db 还没 ready | compose 里加 healthcheck + depends_on.condition |
| 浏览器加载 `.wasm` 报 MIME error | Nginx 没配 `application/wasm` | 见第 5.2 节 |
| cargo 编译卡在 `Downloading crates` | 国内网络到 crates.io 慢 | 配置 rsproxy 国内源（第 3 节） |
| 2GB 机器 `cargo build` 被 kill | OOM | 加 swap 或改用方案 B 本地打包 |
| 502 Bad Gateway | 后端挂了 / 端口没监听本机 | `docker compose ps`、确认 `127.0.0.1:3000` 而非 `0.0.0.0:3000` 只暴露本机 |
| 图片上传后访问 404 | RustFS bucket 没建 / 公开读策略没设 | `mc mb` 建桶，必要时 `mc anonymous set download local/culino` |
| 证书 90 天后突然失效 | certbot 自动续期没跑 | `sudo systemctl status certbot.timer`，或手动 `certbot renew --dry-run` |
| CORS 报错 | `CORS_ORIGINS` 没配正式域名 | `.env` 里加正式域名，重启 app 容器 |
| 请求 AI 超时 | Nginx 默认 60s，DeepSeek 可能慢 | `proxy_read_timeout 120s` |

---

## 11. 监控建议（可选进阶）

- **基础监控**：`htop` / `docker stats` 即可应对小规模
- **应用监控**：Prometheus + Grafana，后端暴露 `/metrics`（Axum 可用 `axum-prometheus`）
- **日志聚合**：Loki / ELK，或最简单的 `logrotate` + 定期清理
- **告警**：UptimeRobot（免费）外部探活，挂了发邮件

---

## 12. 简化版：最小可行部署

如果只是给几个朋友试用，按这个走就行：

1. 买一台 2C4G Ubuntu 服务器 + 一个备案域名
2. `curl -fsSL https://get.docker.com | sh`
3. `git clone` 项目，改 `.env` 里的密钥
4. 按第 4.1 删除不该暴露的端口，`docker compose up -d`
5. 装 Nginx + certbot，按第 5.2 配置
6. 本地 `./gradlew :app:wasmJsBrowserDistribution` → `rsync` 到 `/var/www/culino-web`
7. 访问 `https://your-domain.com` 验证

后续再按需加备份、监控、CI/CD。
