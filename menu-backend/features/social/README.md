# menu-social

社交模块，提供菜谱收藏和烹饪记录功能。

## API

### 收藏

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/v1/social/favorites` | 获取收藏列表 | 是 |
| POST | `/api/v1/social/favorites/{recipe_id}` | 收藏菜谱（幂等） | 是 |
| DELETE | `/api/v1/social/favorites/{recipe_id}` | 取消收藏 | 是 |

### 烹饪记录

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/v1/social/cooking-logs` | 获取烹饪记录 | 是 |
| POST | `/api/v1/social/cooking-logs` | 创建烹饪记录 | 是 |
| PUT | `/api/v1/social/cooking-logs/{id}` | 更新烹饪记录 | 是 |
| DELETE | `/api/v1/social/cooking-logs/{id}` | 删除烹饪记录 | 是 |

## 架构

```
handler.rs  →  repo/
                ├── favorite_repo.rs     (FavoriteRepo trait → PgFavoriteRepo)
                └── cooking_log_repo.rs  (CookingLogRepo trait → PgCookingLogRepo)
```

无 Service 层，Handler 直接调用 Repo。所有操作需要登录，且校验用户归属权。
