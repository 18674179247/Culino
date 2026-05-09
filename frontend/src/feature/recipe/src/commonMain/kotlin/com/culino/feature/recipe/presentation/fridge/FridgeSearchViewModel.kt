package com.culino.feature.recipe.presentation.fridge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.common.util.AppResult
import com.culino.feature.ingredient.data.IngredientRepository
import com.culino.feature.recipe.domain.SearchRecipesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FridgeSearchViewModel(
    private val ingredientRepository: IngredientRepository,
    private val searchRecipesUseCase: SearchRecipesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FridgeSearchUiState())
    val uiState: StateFlow<FridgeSearchUiState> = _uiState.asStateFlow()

    init {
        loadIngredients()
    }

    private fun loadIngredients() {
        viewModelScope.launch {
            val ingredientsResult = ingredientRepository.getIngredients()
            val categoriesResult = ingredientRepository.getCategories()

            if (ingredientsResult is AppResult.Success && categoriesResult is AppResult.Success) {
                _uiState.update {
                    it.copy(
                        ingredientsLoadState = IngredientsLoadState.Success(
                            ingredients = ingredientsResult.data,
                            categories = categoriesResult.data
                        )
                    )
                }
            } else {
                val errorMsg = when {
                    ingredientsResult is AppResult.Error -> ingredientsResult.message
                    categoriesResult is AppResult.Error -> categoriesResult.message
                    else -> "加载失败"
                }
                _uiState.update {
                    it.copy(ingredientsLoadState = IngredientsLoadState.Error(errorMsg))
                }
            }
        }
    }

    fun toggleIngredient(id: Int) {
        _uiState.update { state ->
            val newIds = if (id in state.selectedIngredientIds) {
                state.selectedIngredientIds - id
            } else {
                state.selectedIngredientIds + id
            }
            state.copy(selectedIngredientIds = newIds)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(ingredientSearchQuery = query) }
    }

    fun searchRecipes() {
        val selectedIds = _uiState.value.selectedIngredientIds.toList()
        if (selectedIds.isEmpty()) return

        _uiState.update { it.copy(isSearching = true) }
        viewModelScope.launch {
            when (val result = searchRecipesUseCase(ingredientIds = selectedIds)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            searchResults = FridgeResultState.Success(result.data.data),
                            isSearching = false
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            searchResults = FridgeResultState.Error(result.message),
                            isSearching = false
                        )
                    }
                }
            }
        }
    }

    fun clearResults() {
        _uiState.update { it.copy(searchResults = null) }
    }
}
