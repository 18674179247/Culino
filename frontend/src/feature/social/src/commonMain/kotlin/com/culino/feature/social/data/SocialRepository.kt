package com.culino.feature.social.data

import com.culino.common.util.AppResult
import com.culino.framework.network.safeApiCall

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
    override suspend fun getFavorites(): AppResult<List<Favorite>> =
        safeApiCall { api.getFavorites() }

    override suspend fun isFavorited(recipeId: String): AppResult<Boolean> =
        safeApiCall { api.isFavorited(recipeId) }

    override suspend fun addFavorite(recipeId: String): AppResult<Favorite> =
        safeApiCall { api.addFavorite(recipeId) }

    override suspend fun removeFavorite(recipeId: String): AppResult<Boolean> =
        safeApiCall { api.removeFavorite(recipeId) }

    override suspend fun getCookingLogs(): AppResult<List<CookingLog>> =
        safeApiCall { api.getCookingLogs() }

    override suspend fun createCookingLog(request: CreateCookingLogRequest): AppResult<CookingLog> =
        safeApiCall { api.createCookingLog(request) }

    override suspend fun updateCookingLog(
        id: String,
        request: UpdateCookingLogRequest
    ): AppResult<CookingLog> = safeApiCall { api.updateCookingLog(id, request) }

    override suspend fun deleteCookingLog(id: String): AppResult<Boolean> =
        safeApiCall { api.deleteCookingLog(id) }

    override suspend fun toggleLike(recipeId: String): AppResult<Boolean> =
        safeApiCall { api.toggleLike(recipeId) }

    override suspend fun getComments(recipeId: String, page: Int, pageSize: Int): AppResult<CommentListResponse> =
        safeApiCall { api.getComments(recipeId, page, pageSize) }

    override suspend fun createComment(request: CreateCommentRequest): AppResult<RecipeComment> =
        safeApiCall { api.createComment(request) }

    override suspend fun deleteComment(id: String): AppResult<Boolean> =
        safeApiCall { api.deleteComment(id) }
}
