FROM rust:1.86 AS builder
WORKDIR /app

# 可选：国内构建时启用 crates.io 镜像加速（取消下方 4 行注释）
# RUN mkdir -p ~/.cargo && printf '[source.crates-io]\nreplace-with = "rsproxy-sparse"\n[source.rsproxy-sparse]\nregistry = "sparse+https://rsproxy.cn/index/"\n[net]\ngit-fetch-with-cli = true\n' > ~/.cargo/config.toml

# 先拷贝依赖描述文件，利用 Docker 层缓存
COPY Cargo.toml Cargo.lock ./
COPY common/Cargo.toml common/
COPY features/recipe/Cargo.toml features/recipe/
COPY features/ingredient/Cargo.toml features/ingredient/
COPY features/social/Cargo.toml features/social/
COPY features/tool/Cargo.toml features/tool/
COPY features/upload/Cargo.toml features/upload/
COPY features/user/Cargo.toml features/user/
COPY features/ai/Cargo.toml features/ai/

# 创建桩文件使 cargo build 能解析 workspace
RUN mkdir -p src common/src \
    features/recipe/src features/ingredient/src features/social/src \
    features/tool/src features/upload/src features/user/src features/ai/src \
    && echo 'fn main() {}' > src/main.rs \
    && echo '' > common/src/lib.rs \
    && for f in recipe ingredient social tool upload user ai; do \
         echo '' > "features/$f/src/lib.rs"; \
       done \
    && cargo build --release \
    && rm -rf src common/src features/*/src

# 拷贝真实源码并构建
COPY . .
RUN cargo build --release

FROM debian:bookworm-slim
# wget: compose healthcheck 需要；ca-certificates: HTTPS 依赖
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates wget \
    && rm -rf /var/lib/apt/lists/*

# 非 root 运行
RUN groupadd --system --gid 1001 culino \
    && useradd  --system --uid 1001 --gid culino --no-create-home --shell /usr/sbin/nologin culino

COPY --from=builder /app/target/release/culino-backend /usr/local/bin/culino-backend
COPY --from=builder /app/migrations /app/migrations

WORKDIR /app
RUN mkdir -p /app/logs && chown -R culino:culino /app
USER culino

EXPOSE 3000
CMD ["culino-backend"]
