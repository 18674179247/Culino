FROM rust:1.86 AS builder
WORKDIR /app

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
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/target/release/culino-backend /usr/local/bin/culino-backend
COPY --from=builder /app/migrations /app/migrations
WORKDIR /app
EXPOSE 3000
CMD ["culino-backend"]
