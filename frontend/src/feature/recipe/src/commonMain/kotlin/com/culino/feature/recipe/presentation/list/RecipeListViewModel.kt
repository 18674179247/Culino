package com.culino.feature.recipe.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.common.util.AppResult
import com.culino.feature.ingredient.data.IngredientRepository
import com.culino.feature.recipe.domain.GetRandomRecipesUseCase
import com.culino.feature.recipe.domain.SearchRecipesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeListViewModel(
    private val searchRecipesUseCase: SearchRecipesUseCase,
    private val getRandomRecipesUseCase: GetRandomRecipesUseCase,
    private val ingredientRepository: IngredientRepository,
    private val authorId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeListUiState())
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        searchRecipes(page = 1)
        loadFilterData()
    }

    private fun loadFilterData() {
        viewModelScope.launch {
            val tags = when (val r = ingredientRepository.getTags()) {
                is AppResult.Success -> r.data
                is AppResult.Error -> emptyList()
            }
            val ingredients = when (val r = ingredientRepository.getIngredients()) {
                is AppResult.Success -> r.data
                is AppResult.Error -> emptyList()
            }
            val categories = when (val r = ingredientRepository.getCategories()) {
                is AppResult.Success -> r.data
                is AppResult.Error -> emptyList()
            }
            _uiState.update { it.copy(filterData = FilterData(tags, ingredients, categories)) }
        }
    }

    fun search(keyword: String) {
        _uiState.update { it.copy(searchKeyword = keyword) }
        searchJob?.cancel()
        if (keyword.isBlank()) {
            searchRecipes(page = 1)
            return
        }
        searchJob = viewModelScope.launch {
            delay(300L)
            searchRecipes(keyword = keyword, page = 1)
        }
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

            val state = _uiState.value
            when (val result = searchRecipesUseCase(
                keyword.ifBlank { null },
                difficulty,
                authorId,
                page,
                maxCookingTime = state.maxCookingTime,
                tagIds = state.selectedTagIds.ifEmpty { null },
                ingredientIds = state.selectedIngredientIds.ifEmpty { null }
            )) {
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

    fun updateFilters(
        tagIds: List<Int> = _uiState.value.selectedTagIds,
        maxCookingTime: Int? = _uiState.value.maxCookingTime,
        ingredientIds: List<Int> = _uiState.value.selectedIngredientIds
    ) {
        val hasFilter = tagIds.isNotEmpty() || maxCookingTime != null || ingredientIds.isNotEmpty()
        _uiState.update {
            it.copy(
                selectedTagIds = tagIds,
                maxCookingTime = maxCookingTime,
                selectedIngredientIds = ingredientIds,
                isFilterActive = hasFilter
            )
        }
        searchRecipes(keyword = _uiState.value.searchKeyword, difficulty = _uiState.value.selectedDifficulty, page = 1)
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedTagIds = emptyList(),
                maxCookingTime = null,
                selectedIngredientIds = emptyList(),
                isFilterActive = false
            )
        }
        searchRecipes(keyword = _uiState.value.searchKeyword, difficulty = _uiState.value.selectedDifficulty, page = 1)
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
            searchRecipes(
                keyword = _uiState.value.searchKeyword,
                difficulty = _uiState.value.selectedDifficulty,
                page = 1
            )
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
