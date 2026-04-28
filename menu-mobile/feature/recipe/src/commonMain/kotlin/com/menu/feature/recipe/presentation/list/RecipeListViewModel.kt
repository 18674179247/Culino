package com.menu.feature.recipe.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.AppResult
import com.menu.feature.recipe.domain.GetRandomRecipesUseCase
import com.menu.feature.recipe.domain.SearchRecipesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeListViewModel(
    private val searchRecipesUseCase: SearchRecipesUseCase,
    private val getRandomRecipesUseCase: GetRandomRecipesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeListUiState())
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()

    init {
        loadRandomRecipes()
    }

    fun loadRandomRecipes() {
        viewModelScope.launch {
            _uiState.update { it.copy(state = RecipeListState.Loading) }
            when (val result = getRandomRecipesUseCase()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            state = RecipeListState.Success(
                                recipes = result.data,
                                hasMore = false,
                                currentPage = 1
                            )
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(state = RecipeListState.Error(result.message))
                    }
                }
            }
        }
    }

    fun searchRecipes(keyword: String = "", difficulty: String? = null, page: Int = 1) {
        viewModelScope.launch {
            if (page == 1) {
                _uiState.update { it.copy(state = RecipeListState.Loading) }
            }

            when (val result = searchRecipesUseCase(keyword.ifBlank { null }, difficulty, page)) {
                is AppResult.Success -> {
                    val response = result.data
                    val currentRecipes = if (page == 1) {
                        emptyList()
                    } else {
                        (_uiState.value.state as? RecipeListState.Success)?.recipes ?: emptyList()
                    }

                    _uiState.update {
                        it.copy(
                            state = RecipeListState.Success(
                                recipes = currentRecipes + response.data,
                                hasMore = response.data.size >= response.pageSize,
                                currentPage = page
                            ),
                            searchKeyword = keyword,
                            selectedDifficulty = difficulty
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(state = RecipeListState.Error(result.message))
                    }
                }
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value.state as? RecipeListState.Success ?: return
        if (!currentState.hasMore) return

        searchRecipes(
            keyword = _uiState.value.searchKeyword,
            difficulty = _uiState.value.selectedDifficulty,
            page = currentState.currentPage + 1
        )
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            if (_uiState.value.searchKeyword.isBlank() && _uiState.value.selectedDifficulty == null) {
                loadRandomRecipes()
            } else {
                searchRecipes(
                    keyword = _uiState.value.searchKeyword,
                    difficulty = _uiState.value.selectedDifficulty,
                    page = 1
                )
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun updateSearchKeyword(keyword: String) {
        _uiState.update { it.copy(searchKeyword = keyword) }
    }

    fun updateDifficulty(difficulty: String?) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
        searchRecipes(keyword = _uiState.value.searchKeyword, difficulty = difficulty, page = 1)
    }
}
