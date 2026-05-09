package com.culino.feature.social.data

import com.culino.framework.network.ApiClient
import com.culino.framework.network.ApiResponse

interface SocialApi {
    suspend fun getFavorites(): ApiResponse<List<Favorite>>
    suspend fun addFavorite(recipeId: String): ApiResponse<Favorite>
    suspend fun removeFavorite(recipeId: String): ApiResponse<Boolean>

    suspend fun getCookingLogs(): ApiResponse<List<CookingLog>>
    suspend fun createCookingLog(request: CreateCookingLogRequest): ApiResponse<CookingLog>
    suspend fun updateCookingLog(id: String, request: UpdateCookingLogRequest): ApiResponse<CookingLog>
    suspend fun deleteCookingLog(id: String): ApiResponse<Boolean>

    suspend fun toggleLike(recipeId: String): ApiResponse<Boolean>
    suspend fun getComments(recipeId: String, page: Int = 1, pageSize: Int = 20): ApiResponse<CommentListResponse>
    suspend fun createComment(request: CreateCommentRequest): ApiResponse<RecipeComment>
    suspend fun deleteComment(id: String): ApiResponse<Boolean>
}

class SocialApiImpl(private val client: ApiClient) : SocialApi {
    override suspend fun getFavorites(): ApiResponse<List<Favorite>> {
        return client.get("social/favorites")
    }

    override suspend fun addFavorite(recipeId: String): ApiResponse<Favorite> {
        return client.post("social/favorites/$recipeId", Unit)
    }

    override suspend fun removeFavorite(recipeId: String): ApiResponse<Boolean> {
        return client.delete("social/favorites/$recipeId")
    }

    override suspend fun getCookingLogs(): ApiResponse<List<CookingLog>> {
        return client.get("social/cooking-logs")
    }

    override suspend fun createCookingLog(request: CreateCookingLogRequest): ApiResponse<CookingLog> {
        return client.post("social/cooking-logs", request)
    }

    override suspend fun updateCookingLog(
        id: String,
        request: UpdateCookingLogRequest
    ): ApiResponse<CookingLog> {
        return client.put("social/cooking-logs/$id", request)
    }

    override suspend fun deleteCookingLog(id: String): ApiResponse<Boolean> {
        return client.delete("social/cooking-logs/$id")
    }

    override suspend fun toggleLike(recipeId: String): ApiResponse<Boolean> {
        return client.post("social/likes/$recipeId", Unit)
    }

    override suspend fun getComments(recipeId: String, page: Int, pageSize: Int): ApiResponse<CommentListResponse> {
        return client.get("social/comments/recipe/$recipeId", mapOf("page" to "$page", "page_size" to "$pageSize"))
    }

    override suspend fun createComment(request: CreateCommentRequest): ApiResponse<RecipeComment> {
        return client.post("social/comments", request)
    }

    override suspend fun deleteComment(id: String): ApiResponse<Boolean> {
        return client.delete("social/comments/$id")
    }
}
