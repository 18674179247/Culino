package com.menu.feature.social.data

import com.menu.core.common.Result
import com.menu.core.network.ApiResponse

interface SocialRepository {
    suspend fun getFavorites(): Result<List<Favorite>>
    suspend fun addFavorite(recipeId: String): Result<Favorite>
    suspend fun removeFavorite(recipeId: String): Result<Boolean>
    suspend fun getCookingLogs(): Result<List<CookingLog>>
    suspend fun createCookingLog(request: CreateCookingLogRequest): Result<CookingLog>
    suspend fun updateCookingLog(id: String, request: UpdateCookingLogRequest): Result<CookingLog>
    suspend fun deleteCookingLog(id: String): Result<Boolean>
}

class SocialRepositoryImpl(private val api: SocialApi) : SocialRepository {
    override suspend fun getFavorites(): Result<List<Favorite>> = try {
        when (val response = api.getFavorites()) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun addFavorite(recipeId: String): Result<Favorite> = try {
        when (val response = api.addFavorite(recipeId)) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun removeFavorite(recipeId: String): Result<Boolean> = try {
        when (val response = api.removeFavorite(recipeId)) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun getCookingLogs(): Result<List<CookingLog>> = try {
        when (val response = api.getCookingLogs()) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun createCookingLog(request: CreateCookingLogRequest): Result<CookingLog> = try {
        when (val response = api.createCookingLog(request)) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun updateCookingLog(
        id: String,
        request: UpdateCookingLogRequest
    ): Result<CookingLog> = try {
        when (val response = api.updateCookingLog(id, request)) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun deleteCookingLog(id: String): Result<Boolean> = try {
        when (val response = api.deleteCookingLog(id)) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }
}
