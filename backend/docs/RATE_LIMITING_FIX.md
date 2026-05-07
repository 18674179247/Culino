# 限流问题修复总结

## 问题原因

之前登录失败的根本原因是**限流配置过于严格**：

### 原始配置（已移除）

```rust
let rate_limit_config = GovernorConfigBuilder::default()
    .per_second(5)      // 每秒只允许 5 次请求
    .burst_size(10)     // 突发最多 10 次
    .finish()
    .unwrap();
```

### 问题表现

- 开发测试时频繁登录，很容易超过限制
- 触发 `429 Too Many Requests` 错误
- 用户体验差，看起来像是登录功能坏了

## 解决方案

### 1. 调整限流参数（已实施）

```rust
// 新配置：更宽松但仍然安全
let rate_limit_config = GovernorConfigBuilder::default()
    .per_second(10)     // 每秒允许 10 次请求（翻倍）
    .burst_size(20)     // 突发最多 20 次（翻倍）
    .finish()
    .unwrap();
```

**改进点：**

- ✅ 每秒请求数从 5 提升到 10
- ✅ 突发容量从 10 提升到 20
- ✅ 正常使用不会触发限流
- ✅ 仍能有效防止暴力破解（每秒 10 次已经很快了）

### 2. 限流范围明确

**仅对认证接口限流：**

```rust
let user_auth = Router::new()
    .route("/register", axum::routing::post(culino_user::handler::register))
    .route("/login", axum::routing::post(culino_user::handler::login))
    .layer(GovernorLayer { config: rate_limit_config.into() });
```

**不限流的接口：**

- `/api/v1/user/me` - 个人信息
- `/api/v1/user/logout` - 登出
- 所有其他业务接口（菜谱、食材、社交等）

## 技术细节

### 限流算法：令牌桶（Token Bucket）

```
初始状态：桶中有 20 个令牌（burst_size）
每秒补充：10 个令牌（per_second）
每次请求：消耗 1 个令牌

场景 1：正常使用
- 用户每 2 秒登录一次
- 每次消耗 1 令牌，2 秒后补充 20 令牌
- 永远不会触发限流 ✅

场景 2：快速重试
- 用户 1 秒内尝试 5 次登录
- 消耗 5 令牌，桶中还剩 15 令牌
- 不会触发限流 ✅

场景 3：暴力破解
- 攻击者 2 秒内尝试 25 次登录
- 前 20 次成功（消耗完所有令牌）
- 后 5 次被拒绝（429 错误）✅
```

### 基于 IP 的限流

```rust
// tower_governor 自动提取客户端 IP
// 每个 IP 独立计算限流
IP: 192.168.1.100 -> 独立的令牌桶
IP: 192.168.1.101 -> 独立的令牌桶
```

## 验证测试

### 测试 1：正常登录

```bash
# 连续 5 次登录 - 应该全部成功
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/v1/user/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test123"}'
done
```

**预期结果：** 5 次都返回 200 或 401（密码错误），不会有 429

### 测试 2：触发限流

```bash
# 连续 25 次快速请求
for i in {1..25}; do
  curl -X POST http://localhost:8080/api/v1/user/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test123"}'
done
```

**预期结果：**

- 前 20 次：正常响应
- 后 5 次：`429 Too Many Requests`

## 配置建议

### 开发环境（当前配置）

```rust
.per_second(10)
.burst_size(20)
```

- 适合频繁测试
- 不会影响开发体验

### 生产环境（可选调整）

**高流量场景：**

```rust
.per_second(20)
.burst_size(50)
```

**严格安全场景：**

```rust
.per_second(5)
.burst_size(10)
```

## 相关文件

- ✅ `src/router.rs` - 限流配置已添加
- ✅ `Cargo.toml` - `tower_governor = "0.7"` 依赖已存在
- ✅ `docs/RATE_LIMITING.md` - 详细文档

## 总结

1. **问题已解决**：限流配置已调整为更合理的参数
2. **不影响使用**：正常登录不会触发限流
3. **保持安全**：仍能有效防止暴力破解攻击
4. **编译通过**：代码已验证，可以正常运行

现在可以正常登录了！🎉
