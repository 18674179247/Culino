# culino-user

用户模块，提供注册、登录、登出、个人信息管理功能。

## API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/user/register` | 用户注册，返回 JWT Token | 否 |
| POST | `/api/v1/user/login` | 用户登录，返回 JWT Token | 否 |
| POST | `/api/v1/user/logout` | 用户登出，撤销 Token | 是 |
| GET | `/api/v1/user/me` | 获取当前用户信息 | 是 |
| PUT | `/api/v1/user/me` | 更新个人资料（昵称、头像） | 是 |

## 架构

```
handler.rs  →  repo.rs (UserRepo trait → PgUserRepo)
```

无独立 Service 层，业务逻辑（参数校验、密码哈希、JWT 签发）在 Handler 中完成。
