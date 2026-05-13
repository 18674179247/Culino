package com.culino.common.util

/**
 * 通用"加载数据"状态机。
 *
 * 在各 feature 的 presentation 层代替 sealed State 重复定义。
 *
 * 典型使用:
 * ```
 * private val _state = MutableStateFlow<LoadState<List<Foo>>>(LoadState.Loading)
 * val state: StateFlow<LoadState<List<Foo>>> = _state.asStateFlow()
 *
 * _state.value = LoadState.Loading
 * when (val r = repo.x()) {
 *   is AppResult.Success -> _state.value = LoadState.Success(r.data)
 *   is AppResult.Error   -> _state.value = LoadState.Error(r.message)
 * }
 * ```
 *
 * Screen 层:
 * ```
 * when (val s = state.collectAsState().value) {
 *   LoadState.Loading -> LoadingIndicator()
 *   is LoadState.Success -> FooList(s.data)
 *   is LoadState.Error -> ErrorMessage(s.message)
 * }
 * ```
 *
 * 注:存量 feature 仍在用各自的 sealed 状态(FavoritesState / CookingLogState /
 * ShoppingListState / MealPlanState / RecipeListState / RecipeDetailState 等),
 * 已 7 份结构一致的"Loading / Success(data) / Error(message)"。
 * 新增 feature / 新视图一律用 LoadState<T>,存量待后续迁移。
 */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Success<T>(val data: T) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}

/** AppResult<T> → LoadState<T> 适配 */
fun <T> AppResult<T>.toLoadState(): LoadState<T> = when (this) {
    is AppResult.Success -> LoadState.Success(data)
    is AppResult.Error -> LoadState.Error(message)
}
