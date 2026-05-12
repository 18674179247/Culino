package com.culino.feature.social.data

import com.culino.common.util.AppResult
import com.culino.framework.network.ApiResponse

interface SocialRepository {
    suspend fun getFavorites(): AppResult<List<Favorite>>
    suspend fun isFavorited(recipeId: String): AppResult<Boolean>
    suspend fun addFavorite(recipeId: String): AppResult<Favorite>
    suspend fun removeFavorite(recipeId: String): AppResult<Boolean>
    suspend fun getCookingLogs(): AppResult<List<CookingLog>>
    suspend fun createCookingLog(request: CreateCookingLogRequest): AppResult<CookingLog>
    suspend fun updateCookingLog(id: String, request: UpdateCookingLogRequest): AppResult<CookingLog>
    suspend fun deleteCookingLog(id: String): AppResult<Boolean>
    suspend fun toggleLike(recipeId: String): AppResult<Boolean>
    suspend fun getComments(recipeId: String, page: Int = 1, pageSize: Int = 20): AppResult<CommentListResponse>
    suspend fun createComment(request: CreateCommentRequest): AppResult<RecipeComment>
    suspend fun deleteComment(id: String): AppResult<Boolean>
}

class SocialRepositoryImpl(private val api: SocialApi) : SocialRepository {
    override suspend fun getFavorites(): AppResult<List<Favorite>> = try {
        when (val response = api.getFavorites()) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun isFavorited(recipeId: String): AppResult<Boolean> = try {
        when (val response = api.isFavorited(recipeId)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun addFavorite(recipeId: String): AppResult<Favorite> = try {
        when (val response = api.addFavorite(recipeId)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun removeFavorite(recipeId: String): AppResult<Boolean> = try {
        when (val response = api.removeFavorite(recipeId)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun getCookingLogs(): AppResult<List<CookingLog>> = try {
        when (val response = api.getCookingLogs()) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun createCookingLog(request: CreateCookingLogRequest): AppResult<CookingLog> = try {
        when (val response = api.createCookingLog(request)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun updateCookingLog(
        id: String,
        request: UpdateCookingLogRequest
    ): AppResult<CookingLog> = try {
        when (val response = api.updateCookingLog(id, request)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun deleteCookingLog(id: String): AppResult<Boolean> = try {
        when (val response = api.deleteCookingLog(id)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun toggleLike(recipeId: String): AppResult<Boolean> = try {
        when (val response = api.toggleLike(recipeId)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun getComments(recipeId: String, page: Int, pageSize: Int): AppResult<CommentListResponse> = try {
        when (val response = api.getComments(recipeId, page, pageSize)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun createComment(request: CreateCommentRequest): AppResult<RecipeComment> = try {
        when (val response = api.createComment(request)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun deleteComment(id: String): AppResult<Boolean> = try {
        when (val response = api.deleteComment(id)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }
}
