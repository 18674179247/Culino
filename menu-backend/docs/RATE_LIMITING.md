# 限流配置说明

## 问题分析

之前登录失败的原因是限流配置过于严格：
- **原配置**: 每秒 5 次请求，突发 10 次
- **问题**: 在开发测试时，频繁的登录尝试很容易触发限流，导致 429 Too Many Requests 错误

## 解决方案

### 1. 调整限流参数

```rust
// 新配置：每秒 10 次请求，突发最多 20 次
let rate_limit_config = GovernorConfigBuilder::default()
    .per_second(10)
    .burst_size(20)
    .finish()
    .unwrap();
```

**参数说明:**
- `per_second(10)`: 每秒允许 10 次请求（稳定速率）
- `burst_size(20)`: 突发流量最多 20 次（短时间内的峰值）

这个配置：
- ✅ 足够宽松，不会影响正常使用
- ✅ 仍能有效防止暴力破解攻击
- ✅ 适合开发和生产环境

### 2. 限流范围

限流**仅应用于认证接口**：
- `/api/v1/user/register` - 注册
- `/api/v1/user/login` - 登录

**不限流的接口:**
- `/api/v1/user/me` - 获取个人信息
- `/api/v1/user/logout` - 登出
- 其他所有业务接口

### 3. 限流工作原理

使用 `tower_governor` 基于 IP 地址进行限流：

```
请求 -> 检查 IP -> 计算请求频率 -> 允许/拒绝
                    ↓
            令牌桶算法 (Token Bucket)
            - 每秒补充 10 个令牌
            - 桶容量 20 个令牌
            - 每次请求消耗 1 个令牌
```

## 测试验证

### 正常登录测试
```bash
# 连续 5 次登录请求 - 应该全部成功
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/v1/user/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test123"}'
  echo ""
done
```

### 限流触发测试
```bash
# 连续 25 次请求 - 前 20 次成功，后 5 次会被限流
for i in {1..25}; do
  curl -X POST http://localhost:8080/api/v1/user/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test123"}'
  echo ""
done
```

预期结果：
- 前 20 次：正常响应（200 或 401）
- 后 5 次：`429 Too Many Requests`

## 生产环境建议

根据实际情况调整参数：

### 高流量场景
```rust
.per_second(20)   // 更高的稳定速率
.burst_size(50)   // 更大的突发容量
```

### 严格安全场景
```rust
.per_second(5)    // 更严格的限制
.burst_size(10)   // 更小的突发容量
```

### 按用户限流（可选）
如果需要按用户而非 IP 限流，可以自定义 key extractor：

```rust
use tower_governor::key_extractor::KeyExtractor;

// 自定义按用户 ID 限流
struct UserKeyExtractor;

impl KeyExtractor for UserKeyExtractor {
    type Key = String;
    
    fn extract(&self, req: &Request) -> Result<Self::Key, GovernorError> {
        // 从 JWT token 中提取 user_id
        // ...
    }
}
```

## 监控建议

在生产环境中，建议监控限流指标：

```rust
// 记录限流事件
tracing::warn!(
    ip = ?client_ip,
    path = ?request_path,
    "Rate limit exceeded"
);
```

可以通过日志分析：
- 哪些 IP 频繁触发限流
- 是否有恶意攻击行为
- 是否需要调整限流参数

## 相关文件

- `src/router.rs` - 限流配置
- `Cargo.toml` - `tower_governor = "0.7"` 依赖

## 参考资料

- [tower_governor 文档](https://docs.rs/tower-governor/)
- [令牌桶算法](https://en.wikipedia.org/wiki/Token_bucket)
