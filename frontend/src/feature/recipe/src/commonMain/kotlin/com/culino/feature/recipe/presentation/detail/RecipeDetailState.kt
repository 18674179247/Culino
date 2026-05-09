package com.culino.feature.recipe.presentation.detail

import com.culino.feature.recipe.data.RecipeDetail

sealed interface RecipeDetailState {
    data object Loading : RecipeDetailState
    data class Success(val detail: RecipeDetail) : RecipeDetailState
    data class Error(val message: String) : RecipeDetailState
}
