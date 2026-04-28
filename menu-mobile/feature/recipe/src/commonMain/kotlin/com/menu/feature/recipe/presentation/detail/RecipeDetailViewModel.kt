package com.menu.feature.recipe.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.AppResult
import com.menu.feature.recipe.data.RecipeRepository
import com.menu.feature.recipe.domain.GetRecipeDetailUseCase
import com.menu.feature.social.data.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val getRecipeDetailUseCase: GetRecipeDetailUseCase,
    private val repository: RecipeRepository,
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _state = MutableStateFlow<RecipeDetailState>(RecipeDetailState.Loading)
    val state: StateFlow<RecipeDetailState> = _state.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    private val _favoriteState = MutableStateFlow<FavoriteState>(FavoriteState.Idle)
    val favoriteState: StateFlow<FavoriteState> = _favoriteState.asStateFlow()

    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited.asStateFlow()

    fun loadRecipeDetail(id: String) {
        viewModelScope.launch {
            _state.value = RecipeDetailState.Loading
            when (val result = getRecipeDetailUseCase(id)) {
                is AppResult.Success -> {
                    _state.value = RecipeDetailState.Success(result.data)
                    checkIfFavorited(id)
                }
                is AppResult.Error -> {
                    _state.value = RecipeDetailState.Error(
                        result.message
                    )
                }
            }
        }
    }

    private fun checkIfFavorited(recipeId: String) {
        viewModelScope.launch {
            when (val result = socialRepository.getFavorites()) {
                is AppResult.Success -> {
                    _isFavorited.value = result.data.any { it.recipeId == recipeId }
                }
                is AppResult.Error -> {
                    // 忽略错误,默认未收藏
                    _isFavorited.value = false
                }
            }
        }
    }

    fun toggleFavorite(recipeId: String) {
        viewModelScope.launch {
            _favoriteState.value = FavoriteState.Loading
            val result = if (_isFavorited.value) {
                socialRepository.removeFavorite(recipeId)
            } else {
                socialRepository.addFavorite(recipeId)
            }

            when (result) {
                is AppResult.Success -> {
                    _isFavorited.value = !_isFavorited.value
                    _favoriteState.value = FavoriteState.Success
                }
                is AppResult.Error -> {
                    _favoriteState.value = FavoriteState.Error(result.message)
                }
            }
        }
    }

    fun resetFavoriteState() {
        _favoriteState.value = FavoriteState.Idle
    }

    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Loading
            when (val result = repository.deleteRecipe(id)) {
                is AppResult.Success -> {
                    _deleteState.value = DeleteState.Success
                }
                is AppResult.Error -> {
                    _deleteState.value = DeleteState.Error(result.message)
                }
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }
}

sealed class DeleteState {
    object Idle : DeleteState()
    object Loading : DeleteState()
    object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}

sealed class FavoriteState {
    object Idle : FavoriteState()
    object Loading : FavoriteState()
    object Success : FavoriteState()
    data class Error(val message: String) : FavoriteState()
}
