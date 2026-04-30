package com.menu.feature.recipe.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.AppResult
import com.menu.feature.recipe.data.RecipeDetail
import com.menu.feature.recipe.data.RecipeRepository
import com.menu.feature.recipe.domain.GetRecipeDetailUseCase
import com.menu.feature.social.data.*
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

    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _likeCount = MutableStateFlow(0L)
    val likeCount: StateFlow<Long> = _likeCount.asStateFlow()

    private val _comments = MutableStateFlow<List<RecipeComment>>(emptyList())
    val comments: StateFlow<List<RecipeComment>> = _comments.asStateFlow()

    private val _commentCount = MutableStateFlow(0L)
    val commentCount: StateFlow<Long> = _commentCount.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun clearActionError() { _actionError.value = null }

    fun loadRecipeDetail(id: String) {
        viewModelScope.launch {
            _state.value = RecipeDetailState.Loading
            when (val result = getRecipeDetailUseCase(id)) {
                is AppResult.Success -> {
                    val detail = result.data
                    _state.value = RecipeDetailState.Success(detail)
                    _likeCount.value = detail.likeCount ?: 0
                    _commentCount.value = detail.commentCount ?: 0
                    checkIfFavorited(id)
                    loadComments(id)
                }
                is AppResult.Error -> {
                    _state.value = RecipeDetailState.Error(result.message)
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
                is AppResult.Error -> _isFavorited.value = false
            }
        }
    }

    fun toggleFavorite(recipeId: String) {
        viewModelScope.launch {
            val result = if (_isFavorited.value) {
                socialRepository.removeFavorite(recipeId)
            } else {
                socialRepository.addFavorite(recipeId)
            }
            when (result) {
                is AppResult.Success -> _isFavorited.value = !_isFavorited.value
                is AppResult.Error -> _actionError.value = result.message
            }
        }
    }

    fun toggleLike(recipeId: String) {
        viewModelScope.launch {
            when (val result = socialRepository.toggleLike(recipeId)) {
                is AppResult.Success -> {
                    _isLiked.value = result.data
                    _likeCount.value += if (result.data) 1 else -1
                }
                is AppResult.Error -> _actionError.value = result.message
            }
        }
    }

    fun loadComments(recipeId: String, page: Int = 1) {
        viewModelScope.launch {
            when (val result = socialRepository.getComments(recipeId, page)) {
                is AppResult.Success -> {
                    _comments.value = result.data.data
                    _commentCount.value = result.data.total
                }
                is AppResult.Error -> {}
            }
        }
    }

    fun postComment(recipeId: String, content: String) {
        viewModelScope.launch {
            when (val result = socialRepository.createComment(CreateCommentRequest(recipeId, content))) {
                is AppResult.Success -> {
                    _comments.value = listOf(result.data) + _comments.value
                    _commentCount.value += 1
                }
                is AppResult.Error -> _actionError.value = result.message
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            when (val result = socialRepository.deleteComment(commentId)) {
                is AppResult.Success -> {
                    _comments.value = _comments.value.filter { it.id != commentId }
                    _commentCount.value -= 1
                }
                is AppResult.Error -> _actionError.value = result.message
            }
        }
    }

    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Loading
            when (val result = repository.deleteRecipe(id)) {
                is AppResult.Success -> _deleteState.value = DeleteState.Success
                is AppResult.Error -> _deleteState.value = DeleteState.Error(result.message)
            }
        }
    }

    fun resetDeleteState() { _deleteState.value = DeleteState.Idle }
}

sealed class DeleteState {
    object Idle : DeleteState()
    object Loading : DeleteState()
    object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}
