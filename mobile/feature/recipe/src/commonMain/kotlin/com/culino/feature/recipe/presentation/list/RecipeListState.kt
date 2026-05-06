package com.culino.feature.recipe.presentation.list

import com.culino.feature.recipe.data.RecipeListItem

sealed interface RecipeListState {
    data object Loading : RecipeListState
    data class Success(
        val recipes: List<RecipeListItem>,
        val hasMore: Boolean,
        val currentPage: Int
    ) : RecipeListState
    data class Error(val message: String) : RecipeListState
}

data class RecipeListUiState(
    val state: RecipeListState = RecipeListState.Loading,
    val searchKeyword: String = "",
    val selectedDifficulty: String? = null,
    val isRefreshing: Boolean = false,
    val selectedTagIds: List<Int> = emptyList(),
    val maxCookingTime: Int? = null,
    val selectedIngredientIds: List<Int> = emptyList(),
    val isFilterActive: Boolean = false
)
