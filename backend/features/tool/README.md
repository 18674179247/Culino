# culino-tool

工具模块，提供购物清单和膳食计划管理功能。

## API

### 购物清单

| 方法     | 路径                                                      | 说明           | 认证 |
|--------|---------------------------------------------------------|--------------|----|
| GET    | `/api/v1/tool/shopping-lists`                           | 获取购物清单列表     | 是  |
| POST   | `/api/v1/tool/shopping-lists`                           | 创建购物清单       | 是  |
| GET    | `/api/v1/tool/shopping-lists/{id}`                      | 获取清单详情（含清单项） | 是  |
| DELETE | `/api/v1/tool/shopping-lists/{id}`                      | 删除购物清单       | 是  |
| POST   | `/api/v1/tool/shopping-lists/{id}/items`                | 添加清单项        | 是  |
| POST   | `/api/v1/tool/shopping-lists/{id}/items/batch`          | 批量添加清单项      | 是  |
| PUT    | `/api/v1/tool/shopping-lists/{list_id}/items/{item_id}` | 更新清单项        | 是  |
| DELETE | `/api/v1/tool/shopping-lists/{list_id}/items/{item_id}` | 删除清单项        | 是  |

### 膳食计划

| 方法     | 路径                             | 说明            | 认证 |
|--------|--------------------------------|---------------|----|
| GET    | `/api/v1/tool/meal-plans`      | 获取膳食计划（按日期范围） | 是  |
| POST   | `/api/v1/tool/meal-plans`      | 创建膳食计划        | 是  |
| PUT    | `/api/v1/tool/meal-plans/{id}` | 更新膳食计划        | 是  |
| DELETE | `/api/v1/tool/meal-plans/{id}` | 删除膳食计划        | 是  |

## 架构

```
handler.rs  →  repo/
                ├── shopping_repo.rs   (ShoppingRepo trait → PgShoppingRepo)
                └── meal_plan_repo.rs  (MealPlanRepo trait → PgMealPlanRepo)
```

无 Service 层，Handler 直接调用 Repo。所有操作需要登录，且校验用户归属权。
