package com.culino.feature.recipe.presentation.fridge

import com.culino.feature.ingredient.data.Ingredient
import com.culino.feature.ingredient.data.IngredientCategory
import com.culino.feature.recipe.data.RecipeListItem

data class FridgeSearchUiState(
    val ingredientsLoadState: IngredientsLoadState = IngredientsLoadState.Loading,
    val selectedIngredientIds: Set<Int> = emptySet(),
    val ingredientSearchQuery: String = "",
    val searchResults: FridgeResultState? = null,
    val isSearching: Boolean = false
)

sealed interface IngredientsLoadState {
    data object Loading : IngredientsLoadState
    data class Success(
        val ingredients: List<Ingredient>,
        val categories: List<IngredientCategory>
    ) : IngredientsLoadState
    data class Error(val message: String) : IngredientsLoadState
}

sealed interface FridgeResultState {
    data class Success(val recipes: List<RecipeListItem>) : FridgeResultState
    data class Error(val message: String) : FridgeResultState
}
