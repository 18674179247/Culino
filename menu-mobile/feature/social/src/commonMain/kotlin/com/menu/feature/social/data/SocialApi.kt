package com.menu.feature.social.data

import com.menu.core.network.ApiClient
import com.menu.core.network.ApiResponse

interface SocialApi {
    suspend fun getFavorites(): ApiResponse<List<Favorite>>
    suspend fun addFavorite(recipeId: String): ApiResponse<Favorite>
    suspend fun removeFavorite(recipeId: String): ApiResponse<Boolean>

    suspend fun getCookingLogs(): ApiResponse<List<CookingLog>>
    suspend fun createCookingLog(request: CreateCookingLogRequest): ApiResponse<CookingLog>
    suspend fun updateCookingLog(id: String, request: UpdateCookingLogRequest): ApiResponse<CookingLog>
    suspend fun deleteCookingLog(id: String): ApiResponse<Boolean>
}

class SocialApiImpl(private val client: ApiClient) : SocialApi {
    override suspend fun getFavorites(): ApiResponse<List<Favorite>> {
        return client.get("/social/favorites")
    }

    override suspend fun addFavorite(recipeId: String): ApiResponse<Favorite> {
        return client.post("/social/favorites/$recipeId", Unit)
    }

    override suspend fun removeFavorite(recipeId: String): ApiResponse<Boolean> {
        return client.delete("/social/favorites/$recipeId")
    }

    override suspend fun getCookingLogs(): ApiResponse<List<CookingLog>> {
        return client.get("/social/cooking-logs")
    }

    override suspend fun createCookingLog(request: CreateCookingLogRequest): ApiResponse<CookingLog> {
        return client.post("/social/cooking-logs", request)
    }

    override suspend fun updateCookingLog(
        id: String,
        request: UpdateCookingLogRequest
    ): ApiResponse<CookingLog> {
        return client.put("/social/cooking-logs/$id", request)
    }

    override suspend fun deleteCookingLog(id: String): ApiResponse<Boolean> {
        return client.delete("/social/cooking-logs/$id")
    }
}
