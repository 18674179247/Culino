# ADR 0001 — 前端模块边界整修(F-B2 / F-B3 / F-B5)

**状态:** 提议
**日期:** 2026-05-13
**上下文:** `chore/full-review-fix` 分支全面审查整改

## 背景

4 路并行代理 code review 在 `frontend/src/common/` 发现以下边界问题:

1. **`common/api` 混入业务** — `AiApiService.kt`(178 行)和 `ImageUploadApi.kt` 都是业务 API,不应与 `UploadResponse` 同住 `common/api`
2. **`common/model` 含非全局模型** — `AI.kt`(RecipeNutrition / RecommendationItem)只被 recipe 用;`InviteCode.kt` 只被 user 用
3. **`common/ui` 混入业务组件** — `NutritionCard.kt`、`RecommendationList.kt` 依赖 `common.model` 的业务实体
4. **UseCase 层大量单行转发** — `RecipeUseCases.kt` 4 条、`GetProfileUseCase` 多条、`InviteCodeUseCases` 几条都是 `suspend operator fun invoke(...) = repository.xxx(...)`

## 决策

这些问题属于**架构重构**范畴,不是代码整改:

- F-B2(业务下沉)需要新建 `feature/ai` 模块、迁移 ~5 个文件、修改 ~20 处 import
- F-B3(模型下沉)需要改动 `InviteCode.kt` / `AI.kt` 的包名,影响链式 import
- F-B5(删 UseCase)需要同步改 ViewModel / AppComponent / Factory,一次性爆炸面

当前 `chore/full-review-fix` 分支已经集中了 Wave 0-3 约 40 条改动,**再堆叠这三条会模糊 code review 的边界**,也会让这个 PR 难以回滚。

**结论**:本分支**不做 F-B2 / F-B3 / F-B5**,留作独立迭代:

### 下一步提议(按独立性排序)

1. **PR A — 新建 `feature/ai` 模块**
   - 搬迁 `AiApiService`、`AI.kt`(RecipeNutrition / RecommendationItem)、`NutritionCard`、`RecommendationList`
   - 保持对外接口不变,只做物理迁移
   - 规模:M(涉及 import 改动 ~20 处)

2. **PR B — `ImageUploadApi` 迁 `framework/media`**
   - `framework/media` 已创建(见 commit `bcb48a1`)
   - 把 ImageUploadApi / UploadResponse 从 `common/api` 搬到 `framework/media/upload`
   - 规模:S

3. **PR C — `InviteCode.kt` 迁 `feature/user/data`**
   - 只被 user feature 用,不是跨 feature 共享
   - 规模:S

4. **PR D — 清理纯转发 UseCase**
   - 选择方案:(a) 全删,ViewModel 直接依赖 Repository;(b) 强制有逻辑的 UseCase(目前只有 `LoginUseCase` 的 blank 校验和 `ProfileViewModel` 的组合,数量少)
   - 建议 (a),删除率约 80%
   - 规模:M

## 未列入的理由

- **F-B1 AppResult 单一响应**:Wave 2 F-A1 `safeApiCall` 已经在 Repository 边界把 ApiResponse 全部收成 AppResult,ViewModel 层已经是纯 `AppResult<T>`。`ApiResponse` 只在 `framework/network` 和 `common/api/ImageUploadApi` 内部流转,清理收益已不大。
- **F-B6 ProfileViewModel 横向耦合**:Wave 1 F-S6 新增了 `/user/me/stats` 后端接口,`ProfileViewModel` 不再需要并发拉 `favorites + cooking_logs + recipes`,对 social/recipe Repository 的直接依赖已大幅减弱。完整拆解到 `feature:profile` 需要与 F-B2 / F-B5 一起做。

## 影响

保留 common/ 边界问题若干条,但文档化了后续路径,确保:

1. 新增 feature 不会继续往 common/api 或 common/model 里塞业务
2. UseCase 保留但有清晰的"要么删要么补"路径
3. 下个迭代 PR A-D 独立可并行,风险可控

## 参考

- 审查报告见 commit `40681a5` 起始的 `chore/full-review-fix` 分支
- 相关落地改动:F-B4 `bcb48a1`(ImagePicker 迁移,与 PR B 模式一致)
