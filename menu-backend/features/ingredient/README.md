# menu-ingredient

食材模块，管理食材、调料、标签等配料相关数据。

## API

### 食材

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ingredient/ingredients` | 获取所有食材 |
| GET | `/api/v1/ingredient/ingredients/{id}` | 获取食材详情 |
| POST | `/api/v1/ingredient/ingredients` | 创建食材 |
| PUT | `/api/v1/ingredient/ingredients/{id}` | 更新食材 |
| DELETE | `/api/v1/ingredient/ingredients/{id}` | 删除食材 |
| GET | `/api/v1/ingredient/ingredient-categories` | 获取食材分类 |

### 调料

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ingredient/seasonings` | 获取所有调料 |
| POST | `/api/v1/ingredient/seasonings` | 创建调料 |
| PUT | `/api/v1/ingredient/seasonings/{id}` | 更新调料 |
| DELETE | `/api/v1/ingredient/seasonings/{id}` | 删除调料 |

### 标签

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ingredient/tags` | 获取标签列表（可按类型筛选） |
| POST | `/api/v1/ingredient/tags` | 创建标签 |
| PUT | `/api/v1/ingredient/tags/{id}` | 更新标签 |
| DELETE | `/api/v1/ingredient/tags/{id}` | 删除标签 |

## 架构

```
handler/
├── ingredient.rs  →  repo/ingredient_repo.rs (IngredientRepo trait → PgIngredientRepo)
├── seasoning.rs   →  repo/seasoning_repo.rs  (SeasoningRepo trait → PgSeasoningRepo)
└── tag.rs         →  repo/tag_repo.rs        (TagRepo trait → PgTagRepo)
```

无 Service 层，Handler 直接调用 Repo。
