package com.culino.feature.recipe.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.common.util.AppResult
import com.culino.feature.recipe.data.RecipeDetail
import com.culino.feature.recipe.data.RecipeRepository
import com.culino.feature.recipe.domain.GetRecipeDetailUseCase
import com.culino.feature.social.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeDetailUiState(
    val contentState: RecipeDetailState = RecipeDetailState.Loading,
    val deleteState: DeleteState = DeleteState.Idle,
    val isFavorited: Boolean = false,
    val isLiked: Boolean = false,
    val likeCount: Long = 0,
    val comments: List<RecipeComment> = emptyList(),
    val commentCount: Long = 0,
    val actionError: String? = null
)

class RecipeDetailViewModel(
    private val getRecipeDetailUseCase: GetRecipeDetailUseCase,
    private val repository: RecipeRepository,
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    fun clearActionError() { _uiState.update { it.copy(actionError = null) } }

    fun loadRecipeDetail(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(contentState = RecipeDetailState.Loading) }
            when (val result = getRecipeDetailUseCase(id)) {
                is AppResult.Success -> {
                    val detail = result.data
                    _uiState.update {
                        it.copy(
                            contentState = RecipeDetailState.Success(detail),
                            likeCount = detail.likeCount ?: 0,
                            commentCount = detail.commentCount ?: 0
                        )
                    }
                    checkIfFavorited(id)
                    loadComments(id)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(contentState = RecipeDetailState.Error(result.message)) }
                }
            }
        }
    }

    private fun checkIfFavorited(recipeId: String) {
        viewModelScope.launch {
            when (val result = socialRepository.isFavorited(recipeId)) {
                is AppResult.Success -> _uiState.update { it.copy(isFavorited = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(isFavorited = false) }
            }
        }
    }

    private var favoriteJob: Job? = null
    private var likeJob: Job? = null

    fun toggleFavorite(recipeId: String) {
        val prev = _uiState.value.isFavorited
        _uiState.update { it.copy(isFavorited = !prev) }
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            val result = if (prev) {
                socialRepository.removeFavorite(recipeId)
            } else {
                socialRepository.addFavorite(recipeId)
            }
            if (result is AppResult.Error) {
                _uiState.update { it.copy(isFavorited = prev, actionError = result.message) }
            }
        }
    }

    fun toggleLike(recipeId: String) {
        val prevLiked = _uiState.value.isLiked
        val prevCount = _uiState.value.likeCount
        _uiState.update { it.copy(isLiked = !prevLiked, likeCount = prevCount + if (prevLiked) -1 else 1) }
        likeJob?.cancel()
        likeJob = viewModelScope.launch {
            when (val result = socialRepository.toggleLike(recipeId)) {
                is AppResult.Success -> {
                    val serverLiked = result.data
                    if (serverLiked != _uiState.value.isLiked) {
                        _uiState.update {
                            it.copy(
                                isLiked = serverLiked,
                                likeCount = prevCount + if (serverLiked == prevLiked) 0 else if (serverLiked) 1 else -1
                            )
                        }
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLiked = prevLiked, likeCount = prevCount, actionError = result.message) }
                }
            }
        }
    }

    fun loadComments(recipeId: String, page: Int = 1) {
        viewModelScope.launch {
            when (val result = socialRepository.getComments(recipeId, page)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(comments = result.data.data, commentCount = result.data.total) }
                }
                is AppResult.Error -> {}
            }
        }
    }

    fun postComment(recipeId: String, content: String) {
        viewModelScope.launch {
            when (val result = socialRepository.createComment(CreateCommentRequest(recipeId, content))) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(comments = listOf(result.data) + it.comments, commentCount = it.commentCount + 1)
                    }
                }
                is AppResult.Error -> _uiState.update { it.copy(actionError = result.message) }
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            when (val result = socialRepository.deleteComment(commentId)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(comments = it.comments.filter { c -> c.id != commentId }, commentCount = it.commentCount - 1)
                    }
                }
                is AppResult.Error -> _uiState.update { it.copy(actionError = result.message) }
            }
        }
    }

    fun deleteRecipe(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(deleteState = DeleteState.Loading) }
            when (val result = repository.deleteRecipe(id)) {
                is AppResult.Success -> _uiState.update { it.copy(deleteState = DeleteState.Success) }
                is AppResult.Error -> _uiState.update { it.copy(deleteState = DeleteState.Error(result.message)) }
            }
        }
    }

    fun resetDeleteState() { _uiState.update { it.copy(deleteState = DeleteState.Idle) } }
}

sealed class DeleteState {
    object Idle : DeleteState()
    object Loading : DeleteState()
    object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}
