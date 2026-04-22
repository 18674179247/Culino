# menu-recipe

菜谱模块，提供菜谱的创建、编辑、删除、搜索、随机推荐功能。

菜谱包含关联数据：食材、调料、步骤、标签，创建和更新使用事务保证一致性。

## API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/recipe` | 创建菜谱 | 是 |
| GET | `/api/v1/recipe/search` | 多条件搜索（关键词、难度、时间、标签、食材） | 否 |
| GET | `/api/v1/recipe/random` | 随机推荐菜谱 | 否 |
| GET | `/api/v1/recipe/{id}` | 获取菜谱详情 | 否 |
| PUT | `/api/v1/recipe/{id}` | 更新菜谱（仅作者） | 是 |
| DELETE | `/api/v1/recipe/{id}` | 删除菜谱（仅作者） | 是 |

## 架构

```
handler.rs  →  service.rs  →  repo.rs (RecipeRepo trait → PgRecipeRepo)
```

Service 层负责参数校验和业务编排。
