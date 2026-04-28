package com.menu.feature.recipe.presentation.detail

import com.menu.feature.recipe.data.RecipeDetail

sealed interface RecipeDetailState {
    data object Loading : RecipeDetailState
    data class Success(val detail: RecipeDetail) : RecipeDetailState
    data class Error(val message: String) : RecipeDetailState
}
