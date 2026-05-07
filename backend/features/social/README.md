# culino-social

社交模块，提供菜谱收藏、烹饪记录、点赞和评论功能。

## API

### 收藏

| 方法     | 路径                                     | 说明            | 认证 |
|--------|----------------------------------------|---------------|----|
| GET    | `/api/v1/social/favorites`             | 获取收藏列表（含菜谱摘要） | 是  |
| POST   | `/api/v1/social/favorites/{recipe_id}` | 收藏菜谱（幂等）      | 是  |
| DELETE | `/api/v1/social/favorites/{recipe_id}` | 取消收藏          | 是  |

### 烹饪记录

| 方法     | 路径                                 | 说明     | 认证 |
|--------|------------------------------------|--------|----|
| GET    | `/api/v1/social/cooking-logs`      | 获取烹饪记录 | 是  |
| POST   | `/api/v1/social/cooking-logs`      | 创建烹饪记录 | 是  |
| PUT    | `/api/v1/social/cooking-logs/{id}` | 更新烹饪记录 | 是  |
| DELETE | `/api/v1/social/cooking-logs/{id}` | 删除烹饪记录 | 是  |

### 点赞

| 方法   | 路径                                 | 说明              | 认证 |
|------|------------------------------------|-----------------|----|
| POST | `/api/v1/social/likes/{recipe_id}` | 点赞/取消点赞（Toggle） | 是  |

### 评论

| 方法     | 路径                                           | 说明         | 认证 |
|--------|----------------------------------------------|------------|----|
| GET    | `/api/v1/social/comments/recipe/{recipe_id}` | 获取评论列表（分页） | 否  |
| POST   | `/api/v1/social/comments`                    | 发表评论       | 是  |
| DELETE | `/api/v1/social/comments/{id}`               | 删除评论（仅作者）  | 是  |

## 架构

```
handler.rs  →  repo/
                ├── favorite_repo.rs     (FavoriteRepo trait → PgFavoriteRepo)
                ├── cooking_log_repo.rs  (CookingLogRepo trait → PgCookingLogRepo)
                ├── like_repo.rs         (LikeRepo — 静态方法)
                └── comment_repo.rs      (CommentRepo — 静态方法)
```

无 Service 层，Handler 直接调用 Repo。收藏和烹饪记录操作会异步记录用户行为日志（fire-and-forget），供 AI 偏好分析使用。
