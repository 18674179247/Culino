package com.menu.feature.recipe.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.Result
import com.menu.feature.recipe.domain.GetRecipeDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val getRecipeDetailUseCase: GetRecipeDetailUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<RecipeDetailState>(RecipeDetailState.Loading)
    val state: StateFlow<RecipeDetailState> = _state.asStateFlow()

    fun loadRecipeDetail(id: String) {
        viewModelScope.launch {
            _state.value = RecipeDetailState.Loading
            when (val result = getRecipeDetailUseCase(id)) {
                is Result.Success -> {
                    _state.value = RecipeDetailState.Success(result.data)
                }
                is Result.Error -> {
                    _state.value = RecipeDetailState.Error(
                        result.exception.message ?: "加载失败"
                    )
                }
            }
        }
    }
}
