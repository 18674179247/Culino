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
            when (val result = socialRepository.isFavorited(recipeId)) {
                is AppResult.Success -> _isFavorited.value = result.data
                is AppResult.Error -> _isFavorited.value = false
            }
        }
    }

    // 同一菜谱的 toggle 正在飞行时,下一次点击先取消上一次,避免连点造成"点两次抵消了"的乱序结果
    private var favoriteJob: Job? = null
    private var likeJob: Job? = null

    fun toggleFavorite(recipeId: String) {
        val prev = _isFavorited.value
        // 乐观 UI: 先翻转本地状态,给用户即时反馈
        _isFavorited.value = !prev
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            val result = if (prev) {
                socialRepository.removeFavorite(recipeId)
            } else {
                socialRepository.addFavorite(recipeId)
            }
            if (result is AppResult.Error) {
                // 回滚并提示,网络失败不让 UI 停留在错误状态
                _isFavorited.value = prev
                _actionError.value = result.message
            }
        }
    }

    fun toggleLike(recipeId: String) {
        val prevLiked = _isLiked.value
        val prevCount = _likeCount.value
        // 乐观 UI: 立即翻转并调整计数
        _isLiked.value = !prevLiked
        _likeCount.value = prevCount + if (prevLiked) -1 else 1
        likeJob?.cancel()
        likeJob = viewModelScope.launch {
            when (val result = socialRepository.toggleLike(recipeId)) {
                is AppResult.Success -> {
                    // 以服务端权威结果为准,乐观值若与 server 不一致则对齐
                    val serverLiked = result.data
                    if (serverLiked != _isLiked.value) {
                        _isLiked.value = serverLiked
                        // 计数按 server 状态 vs 进入时基线计算
                        _likeCount.value = prevCount + if (serverLiked == prevLiked) 0 else if (serverLiked) 1 else -1
                    }
                }
                is AppResult.Error -> {
                    _isLiked.value = prevLiked
                    _likeCount.value = prevCount
                    _actionError.value = result.message
                }
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
