# Menu App AI 功能产品设计文档

## 一、现状问题

### 1.1 营养分析不够清晰
- 营养数值缺少明确的"每份"标注，用户不知道是整道菜还是单人份
- 没有红绿灯标识（高脂高钠 = 红灯，适量 = 黄灯，健康 = 绿灯）
- 缺少对整道菜的综合评价总结

### 1.2 AI 功能覆盖不足
- 创建菜谱全靠手动填写，上传图片后 AI 不能自动识别和补全
- 没有 AI 对话入口，用户无法用自然语言与 app 交互
- 推荐系统存在但前端入口不明显

### 1.3 现有 AI 能力
| 功能 | 后端 | 前端 |
|------|------|------|
| 营养分析 | 已实现（DeepSeek） | NutritionCard 展示 |
| 个性化推荐 | 已实现（4种类型） | RecommendationList 展示 |
| 用户偏好分析 | 已实现 | 有 API 无 UI |
| 行为日志 | 已实现 | 有 API 调用 |
| 图片识别 | 无 | 无 |
| AI 对话 | 无 | 无 |
| 菜谱自动补全 | 无 | 无 |

---

## 二、功能设计

### 功能 1：AI 图片识别 → 自动补全菜谱

**用户场景**：用户上传一张菜品照片，AI 自动识别菜名、食材、步骤，一键填充表单。

**交互流程**：
1. 创建菜谱页面，上传封面图后出现"AI 识别补全"按钮
2. 点击后底部弹窗显示"正在识别..."
3. AI 返回结果后弹窗展示识别内容预览（菜名、食材列表、步骤）
4. 用户点击"填充到表单"，自动填入各字段（不覆盖已填内容，仅补空）
5. 用户可以在表单上继续修改

**后端改动**：

新增 API：`POST /api/v1/ai/recipe/recognize`

请求体：
```json
{
  "image_url": "https://s3.../images/xxx.jpg",
  "existing_title": "可选，已填的菜名",
  "existing_ingredients": ["可选，已填的食材"]
}
```

响应体：
```json
{
  "title": "宫保鸡丁",
  "description": "经典川菜，鸡肉嫩滑，花生酥脆，麻辣鲜香。",
  "difficulty": 3,
  "cooking_time": 25,
  "servings": 2,
  "ingredients": [
    { "name": "鸡胸肉", "amount": "300g" },
    { "name": "花生米", "amount": "50g" },
    { "name": "干辣椒", "amount": "8个" }
  ],
  "seasonings": [
    { "name": "生抽", "amount": "2勺" },
    { "name": "醋", "amount": "1勺" }
  ],
  "steps": [
    "鸡胸肉切丁，加料酒、淀粉腌制15分钟",
    "花生米小火炒熟备用",
    "锅中热油，爆香干辣椒和花椒",
    "放入鸡丁翻炒至变色",
    "倒入调好的酱汁翻炒均匀",
    "出锅前撒入花生米拌匀"
  ],
  "confidence": 0.85
}
```

实现方式：
- `deepseek.rs` 新增 `recognize_recipe_from_image()` 方法
- DeepSeek 支持 vision（图片理解），将图片 URL 作为 image_url 类型的 content 发送
- 如果 DeepSeek 不支持 vision，降级为：用户上传图片 → 提取菜名（让用户确认）→ 根据菜名生成完整菜谱
- 新增 `features/ai/src/recognition.rs` 处理识别逻辑

**前端改动**：

RecipeCreateScreen：
- 上传封面图后显示"AI 智能补全"按钮（带闪光图标）
- 点击触发 API 调用，底部弹窗显示进度和结果预览
- 确认后调用 ViewModel 的 `applyAiRecognition()` 填充表单

RecipeCreateViewModel：
- 新增 `recognizeFromImage()` 方法
- 新增 `applyAiRecognition(result)` 方法，智能合并到 formState

---

### 功能 2：营养分析增强 — 红绿灯 + 单位明确 + 综合评价

**用户场景**：查看菜谱营养信息时，一眼看出这道菜健康不健康，每份含多少营养素。

**改动点**：

#### 2.1 后端 — 增强 DeepSeek prompt

修改 `deepseek.rs` 的营养分析 prompt，新增字段：

```json
{
  "calories": 385,
  "protein": 28.5,
  "fat": 18.2,
  "carbohydrate": 25.0,
  "fiber": 3.2,
  "sodium": 820,
  "serving_size": "每份（约350g）",
  "traffic_light": {
    "fat": "amber",
    "saturated_fat": "green",
    "sugar": "green",
    "sodium": "red"
  },
  "overall_rating": "green",
  "summary": "这道菜蛋白质丰富，脂肪适中，但钠含量偏高。建议减少酱油用量，适合健身人群作为正餐。",
  "health_score": 72,
  "health_tags": ["高蛋白", "低糖"],
  "suitable_for": ["健身人群", "增肌人群"],
  "cautions": ["钠含量偏高，高血压患者慎食"]
}
```

红绿灯标准（参考英国 FSA 标准，每 100g）：
| 营养素 | 绿灯 | 黄灯 | 红灯 |
|--------|------|------|------|
| 脂肪 | ≤3g | 3-17.5g | >17.5g |
| 饱和脂肪 | ≤1.5g | 1.5-5g | >5g |
| 糖 | ≤5g | 5-22.5g | >22.5g |
| 钠 | ≤120mg | 120-600mg | >600mg |

数据库 `recipe_nutrition` 表新增字段：
- `serving_size TEXT` — 每份说明
- `traffic_light JSONB` — 红绿灯数据
- `overall_rating VARCHAR(10)` — 综合评级 green/amber/red
- `summary TEXT` — AI 综合评价

#### 2.2 前端 — NutritionCard 改造

NutritionCard 新增展示：
- 顶部：综合评级徽章（绿/黄/红圆形图标 + "健康"/"适量"/"注意"文字）
- 每份说明文字："以下为每份（约350g）的营养含量"
- 每个营养素旁边加红绿灯圆点指示
- 底部新增"AI 评价"卡片，展示 summary 文字
- cautions 用红色警告样式展示

---

### 功能 3：AI 智能助手（对话式交互）

**用户场景**：用户可以用自然语言问 AI 问题，比如"今晚吃什么""帮我做个减脂食谱""冰箱里有鸡蛋和西红柿能做什么"。

**交互设计**：
- 底部导航栏新增"AI"tab（或首页添加 AI 悬浮按钮）
- 点击进入 AI 对话页面，类似聊天界面
- AI 回复中可以嵌入菜谱卡片（可点击跳转到菜谱详情）
- 支持的意图：

| 用户说 | AI 行为 |
|--------|---------|
| "今晚吃什么" | 根据偏好推荐 3 道菜，附卡片 |
| "帮我做个减脂晚餐" | 推荐低脂菜谱 + 营养分析 |
| "鸡蛋和西红柿能做什么" | 搜索含这些食材的菜谱 |
| "这道菜热量高吗" | 分析当前/指定菜谱营养 |
| "帮我生成一周食谱" | 生成 7 天食谱计划 |
| "把上次做的红烧肉加入收藏" | 执行收藏操作 |

**后端改动**：

新增 API：`POST /api/v1/ai/chat`

请求体：
```json
{
  "message": "今晚吃什么，想吃清淡的",
  "context": {
    "current_recipe_id": "可选，当前浏览的菜谱",
    "history": [
      { "role": "user", "content": "..." },
      { "role": "assistant", "content": "..." }
    ]
  }
}
```

响应体：
```json
{
  "message": "根据你的口味偏好，推荐这几道清淡的菜：",
  "actions": [
    {
      "type": "recipe_cards",
      "recipe_ids": ["uuid1", "uuid2", "uuid3"]
    }
  ]
}
```

action 类型：
- `recipe_cards` — 展示菜谱卡片列表
- `nutrition_analysis` — 展示营养分析结果
- `meal_plan` — 展示一周食谱
- `navigate` — 跳转到指定页面
- `favorite` — 执行收藏操作

实现方式：
- 新增 `features/ai/src/chat.rs`
- System prompt 包含用户偏好、可用 API 列表
- DeepSeek 返回结构化 JSON（message + actions）
- 后端解析 actions 并补充数据（如根据 recipe_ids 查询菜谱信息）

**前端改动**：

新增 `feature/ai/` 模块：
- `AiChatScreen.kt` — 聊天界面
- `AiChatViewModel.kt` — 管理对话状态
- `ChatMessage.kt` — 消息数据模型（文本、菜谱卡片、营养卡片等）
- `ChatBubble.kt` — 消息气泡组件
- `RecipeCardInChat.kt` — 聊天中的菜谱卡片（可点击跳转）

Navigation 新增 AI 对话路由。

---

### 功能 4：智能食谱规划

**用户场景**：用户设定健康目标（减脂/增肌/均衡），AI 生成一周食谱计划。

**后端改动**：

新增 API：`POST /api/v1/ai/meal-plan/generate`

请求体：
```json
{
  "goal": "减脂",
  "days": 7,
  "meals_per_day": 3,
  "calories_target": 1800,
  "exclude_ingredients": ["花生"],
  "preferences": "喜欢川菜，不吃辣"
}
```

响应体：
```json
{
  "plan": [
    {
      "day": 1,
      "date_label": "周一",
      "meals": [
        {
          "meal_type": "breakfast",
          "recipe_id": "已有菜谱ID或null",
          "title": "燕麦水果碗",
          "calories": 350,
          "brief": "高纤维低脂，提供持久饱腹感"
        },
        { "meal_type": "lunch", "..." : "..." },
        { "meal_type": "dinner", "..." : "..." }
      ],
      "daily_total": { "calories": 1780, "protein": 95, "fat": 52, "carb": 210 }
    }
  ],
  "weekly_summary": {
    "avg_calories": 1795,
    "nutrition_balance": "营养均衡，蛋白质充足，脂肪控制良好",
    "tips": "建议每天补充 2L 水，餐间可加一份水果"
  }
}
```

实现方式：
- 新增 `features/ai/src/meal_plan.rs`
- 优先匹配数据库中已有菜谱，不足时 AI 生成建议
- 食谱计划可保存到 `tool` 模块的 meal_plans 表

**前端改动**：

新增 `feature/tool/` 下的食谱规划页面：
- `MealPlanScreen.kt` — 一周食谱展示（日历卡片视图）
- `MealPlanGenerateSheet.kt` — 生成配置底部弹窗
- 每日卡片可展开查看三餐详情，点击菜谱可跳转

---

### 功能 5：AI 烹饪助手（步骤引导）

**用户场景**：做菜时打开菜谱，AI 提供分步语音/文字引导，可以问"现在该放多少盐""火候怎么控制"。

**后端改动**：

新增 API：`POST /api/v1/ai/cooking-assist`

请求体：
```json
{
  "recipe_id": "uuid",
  "current_step": 3,
  "question": "鸡肉怎么判断熟没熟？"
}
```

响应体：
```json
{
  "answer": "用筷子戳鸡肉最厚的部分，如果流出的汁液是透明的就熟了。如果还是粉红色的，再煮2分钟。",
  "tips": ["鸡胸肉容易柴，建议中火煎制，每面3分钟", "可以用肉温计，内部温度达到75°C即可"]
}
```

**前端改动**：

RecipeDetailScreen 新增"开始烹饪"按钮：
- 进入烹饪模式，全屏展示当前步骤
- 底部输入框可以随时提问
- 左右滑动切换步骤
- 每步可显示计时器（如果 step 有 duration）

---

## 三、优先级排序

| 优先级 | 功能 | 开发量 | 用户价值 |
|--------|------|--------|----------|
| P0 | 营养分析增强（红绿灯+评价） | 小 | 高 — 解决当前用户困惑 |
| P0 | AI 图片识别补全菜谱 | 中 | 高 — 大幅降低创建门槛 |
| P1 | AI 智能助手（对话） | 大 | 高 — 核心差异化功能 |
| P1 | AI 烹饪助手 | 中 | 中 — 提升做菜体验 |
| P2 | 智能食谱规划 | 中 | 中 — 健康管理场景 |

---

## 四、技术方案总结

### 后端新增/修改文件

| 文件 | 改动 |
|------|------|
| `features/ai/src/deepseek.rs` | 增强 prompt，新增 vision 调用、chat 方法 |
| `features/ai/src/recognition.rs` | 新增：图片识别菜谱 |
| `features/ai/src/chat.rs` | 新增：AI 对话，意图识别 + action 生成 |
| `features/ai/src/meal_plan.rs` | 新增：食谱规划生成 |
| `features/ai/src/cooking_assist.rs` | 新增：烹饪助手问答 |
| `features/ai/src/nutrition.rs` | 修改：增强 prompt，新增红绿灯字段 |
| `features/ai/src/model.rs` | 修改：新增 serving_size, traffic_light, overall_rating, summary 字段 |
| `features/ai/src/handler.rs` | 修改：新增 5 个 handler |
| `features/ai/src/repo.rs` | 修改：新增存储方法 |
| `src/router.rs` | 修改：注册新路由 |
| 数据库迁移 | recipe_nutrition 表新增 4 个字段 |

### 前端新增/修改文件

| 文件 | 改动 |
|------|------|
| `core/model/.../AI.kt` | 修改：新增 TrafficLight, MealPlan 等模型 |
| `core/network/.../AiApiService.kt` | 修改：新增 5 个 API 方法 |
| `core/ui/.../NutritionCard.kt` | 修改：红绿灯指示、综合评价、单位标注 |
| `feature/recipe/.../create/RecipeCreateScreen.kt` | 修改：AI 补全按钮和流程 |
| `feature/recipe/.../create/RecipeCreateViewModel.kt` | 修改：识别和填充逻辑 |
| `feature/recipe/.../detail/RecipeDetailScreen.kt` | 修改：烹饪模式入口 |
| `feature/ai/.../AiChatScreen.kt` | 新增：AI 对话页面 |
| `feature/ai/.../AiChatViewModel.kt` | 新增：对话状态管理 |
| `feature/ai/.../components/ChatBubble.kt` | 新增：消息气泡 |
| `feature/ai/.../components/RecipeCardInChat.kt` | 新增：聊天内菜谱卡片 |
| `feature/tool/.../MealPlanScreen.kt` | 新增：食谱规划页面 |
| `feature/tool/.../CookingModeScreen.kt` | 新增：烹饪模式页面 |
| `composeApp/.../Navigation.kt` | 修改：新增路由 |

### API 新增汇总

```
POST  /api/v1/ai/recipe/recognize        — 图片识别菜谱
POST  /api/v1/ai/chat                     — AI 对话
POST  /api/v1/ai/meal-plan/generate       — 生成食谱计划
POST  /api/v1/ai/cooking-assist           — 烹饪助手问答
```

### DeepSeek 调用策略

- 所有 AI 接口统一 120s 超时
- 营养分析和图片识别结果缓存到数据库，避免重复调用
- 对话接口不缓存，每次实时调用
- 食谱规划结果保存到 meal_plans 表，用户可反复查看
- 错误降级：DeepSeek 不可用时返回友好提示，不阻塞核心功能
