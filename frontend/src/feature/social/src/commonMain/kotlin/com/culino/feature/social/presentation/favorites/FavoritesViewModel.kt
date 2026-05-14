package com.culino.feature.social.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.common.util.AppResult
import com.culino.feature.social.data.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: SocialRepository
) : ViewModel() {

    private val _state = MutableStateFlow<FavoritesState>(FavoritesState.Loading)
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _state.value = FavoritesState.Loading
            when (val result = repository.getFavorites()) {
                is AppResult.Success -> {
                    _state.value = FavoritesState.Success(result.data)
                }
                is AppResult.Error -> {
                    _state.value = FavoritesState.Error(
                        result.message
                    )
                }
            }
        }
    }

    fun removeFavorite(recipeId: String) {
        viewModelScope.launch {
            when (repository.removeFavorite(recipeId)) {
                is AppResult.Success -> {
                    val current = (_state.value as? FavoritesState.Success)?.favorites ?: return@launch
                    _state.value = FavoritesState.Success(current.filter { it.recipeId != recipeId })
                }
                is AppResult.Error -> {
                    // 可以显示错误提示
                }
            }
        }
    }
}
