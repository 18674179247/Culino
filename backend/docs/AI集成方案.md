# AI 功能集成文档

## 📋 已完成功能

### 阶段 1：数据库扩展 ✅

创建了以下数据表：

1. **recipe_nutrition** - 菜谱营养分析表
   - 营养成分：热量、蛋白质、脂肪、碳水、纤维、钠
   - AI 分析：健康评分、健康标签、适合人群、注意事项

2. **user_preferences** - 用户偏好画像表
   - 偏好数据：喜欢的菜系、口味、食材、标签
   - 用户特征：饮食限制、健康目标、烹饪时间偏好

3. **ai_recommendations** - AI 推荐记录表
   - 推荐类型：个性化、相似、热门、健康目标
   - 推荐分数和理由

4. **user_behavior_logs** - 用户行为日志表
   - 行为类型：浏览、收藏、烹饪、评分、搜索

### 阶段 2：AI 模块开发 ✅

创建了完整的 `culino-ai` feature 模块：

#### 核心服务

1. **NutritionService** - 营养分析服务
   - 调用 DeepSeek API 分析菜谱营养成分
   - 自动生成健康评分和建议

2. **RecommendationService** - 智能推荐系统
   - 个性化推荐：基于用户偏好
   - 相似推荐：基于标签相似度
   - 热门推荐：基于收藏和评分
   - 健康目标推荐：基于营养标签

3. **PreferenceService** - 用户偏好分析
   - 分析收藏菜谱的标签分布
   - 统计食材偏好
   - 计算烹饪时间和难度偏好

#### API 端点

**营养分析：**
- `POST /api/v1/ai/nutrition/analyze/{recipe_id}` - 触发营养分析
- `GET /api/v1/ai/nutrition/{recipe_id}` - 获取营养信息

**推荐系统：**
- `GET /api/v1/ai/recommend/personalized` - 个性化推荐（需登录）
- `GET /api/v1/ai/recommend/similar/{recipe_id}` - 相似菜谱推荐
- `GET /api/v1/ai/recommend/trending` - 热门推荐
- `GET /api/v1/ai/recommend/health/{goal}` - 健康目标推荐（需登录）

**用户偏好：**
- `POST /api/v1/ai/preference/analyze` - 分析用户偏好（需登录）
- `GET /api/v1/ai/preference/profile` - 获取用户偏好画像（需登录）

**行为日志：**
- `POST /api/v1/ai/behavior/log` - 记录用户行为（需登录）

## 🚀 使用指南

### 1. 配置 DeepSeek API Key

在 `.env` 文件中添加：

```bash
DEEPSEEK_API_KEY=your-deepseek-api-key-here
```

获取 API Key：https://platform.deepseek.com/

### 2. 运行数据库迁移

```bash
cargo run -- migrate
```

这将创建所有 AI 相关的数据表。

### 3. 启动服务

```bash
cargo run -- serve
```

### 4. 测试 API

访问 Swagger UI：http://localhost:3000/swagger-ui/

查看所有 AI 相关的 API 文档。

## 📊 使用示例

### 示例 1：分析菜谱营养

```bash
# 创建菜谱后，触发营养分析
curl -X POST http://localhost:3000/api/v1/ai/nutrition/analyze/{recipe_id} \
  -H "Authorization: Bearer YOUR_TOKEN"

# 获取营养信息
curl http://localhost:3000/api/v1/ai/nutrition/{recipe_id}
```

### 示例 2：获取个性化推荐

```bash
# 先分析用户偏好
curl -X POST http://localhost:3000/api/v1/ai/preference/analyze \
  -H "Authorization: Bearer YOUR_TOKEN"

# 获取个性化推荐
curl http://localhost:3000/api/v1/ai/recommend/personalized?limit=10 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 示例 3：记录用户行为

```bash
curl -X POST http://localhost:3000/api/v1/ai/behavior/log \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "recipe_id": "uuid-here",
    "action_type": "view",
    "action_value": null
  }'
```

## 🔄 下一步：阶段 3 - 集成现有系统

需要在以下地方添加集成代码：

### 1. 菜谱创建后自动触发营养分析

在 `culino-backend/features/recipe/src/handler.rs` 的 `create` 函数中：

```rust
pub async fn create(...) -> ApiResult<RecipeDetail> {
    // 原有逻辑
    let detail = svc.create(auth.user_id, &req).await?;

    // 新增：异步触发营养分析
    let pool = state.pool.clone();
    let recipe_id = detail.recipe.id;
    let api_key = state.config.deepseek_api_key.clone();

    tokio::spawn(async move {
        if let Some(key) = api_key {
            if let Ok(ai_svc) = culino_ai::nutrition::NutritionService::new(pool, key) {
                let _ = ai_svc.analyze_recipe_nutrition(recipe_id, false).await;
            }
        }
    });

    ApiResponse::ok(detail)
}
```

### 2. 在菜谱详情中返回营养信息

修改 `RecipeDetail` 结构体，添加营养信息字段。

### 3. 记录用户行为

行为日志通过 `BehaviorLogger` trait（定义在 common）注入 `AppState`，由 `culino-ai` 实现。业务模块（如 social）通过 `state.behavior_logger` 异步记录，无需直接依赖 AI 模块：

```rust
// common/src/behavior.rs
#[async_trait]
pub trait BehaviorLogger: Send + Sync {
    async fn log(&self, user_id: Uuid, recipe_id: Uuid,
        action_type: &str, action_value: Option<serde_json::Value>) -> Result<(), AppError>;
}

// features/social/src/handler.rs
fn spawn_behavior_log(state: &AppState, user_id: Uuid, recipe_id: Uuid,
    action: &'static str, value: Option<serde_json::Value>,
) {
    if let Some(logger) = state.behavior_logger.clone() {
        tokio::spawn(async move {
            if let Err(e) = logger.log(user_id, recipe_id, action, value).await {
                tracing::error!("行为日志记录失败: ...");
            }
        });
    }
}
```

自动记录的行为：
- 收藏菜谱 → `action_type: "favorite"`
- 取消收藏 → `action_type: "unfavorite"`
- 创建烹饪记录 → `action_type: "cook"`（含评分）
- 前端也可主动调用 `POST /api/v1/ai/behavior/log` 记录浏览/搜索等行为

## 📱 前端集成建议

### 1. 营养信息展示

在菜谱详情页添加营养信息卡片：

```kotlin
// 营养成分图表
NutritionChart(
    calories = nutrition.calories,
    protein = nutrition.protein,
    fat = nutrition.fat,
    carbohydrate = nutrition.carbohydrate
)

// 健康评分
HealthScoreBadge(score = nutrition.healthScore)

// 健康标签
HealthTags(tags = nutrition.healthTags)
```

### 2. 推荐页面

创建推荐页面，展示：
- "为你推荐"（个性化）
- "相似菜谱"（基于当前浏览）
- "热门菜谱"（趋势）
- 每个推荐显示推荐理由

### 3. 用户偏好页面

展示用户的：
- 喜欢的菜系分布
- 喜欢的食材
- 烹饪习惯统计

## 🎯 功能特点

1. **自动化营养分析**
   - 基于 DeepSeek V4 AI 模型
   - 自动计算营养成分
   - 生成健康建议

2. **智能推荐系统**
   - 多维度推荐策略
   - 基于用户行为学习
   - 实时更新推荐

3. **用户偏好分析**
   - 自动分析用户喜好
   - 无需手动设置
   - 持续优化

4. **行为追踪**
   - 记录用户所有交互
   - 用于改进推荐算法
   - 支持数据分析

## ⚠️ 注意事项

1. **API Key 安全**
   - 不要将 API Key 提交到版本控制
   - 生产环境使用环境变量

2. **性能考虑**
   - 营养分析是异步执行的
   - 首次分析可能需要几秒钟
   - 结果会被缓存

3. **成本控制**
   - DeepSeek API 按调用次数计费
   - 建议设置缓存策略
   - 避免重复分析

## 📈 未来优化方向

1. **批量分析**
   - 支持批量分析多个菜谱
   - 提高效率

2. **缓存优化**
   - Redis 缓存热门推荐
   - 减少数据库查询

3. **推荐算法优化**
   - ✅ 时间衰减因子（已实现：新菜谱 30 天内有曝光加分）
   - 引入协同过滤
   - 考虑季节性因素

4. **A/B 测试**
   - 测试不同推荐策略
   - 优化推荐效果
