package com.culino.feature.recipe.data

import com.culino.core.common.AppResult
import com.culino.core.network.ApiResponse

interface RecipeRepository {
    suspend fun searchRecipes(
        keyword: String? = null,
        difficulty: String? = null,
        authorId: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
        maxCookingTime: Int? = null,
        tagIds: List<Int>? = null,
        ingredientIds: List<Int>? = null
    ): AppResult<PaginatedResponse<RecipeListItem>>

    suspend fun getRecipeDetail(id: String): AppResult<RecipeDetail>

    suspend fun createRecipe(request: CreateRecipeRequest): AppResult<RecipeDetail>

    suspend fun updateRecipe(id: String, request: CreateRecipeRequest): AppResult<RecipeDetail>

    suspend fun deleteRecipe(id: String): AppResult<Boolean>

    suspend fun getRandomRecipes(count: Int = 5): AppResult<List<RecipeListItem>>
}

class RecipeRepositoryImpl(private val api: RecipeApi) : RecipeRepository {
    override suspend fun searchRecipes(
        keyword: String?,
        difficulty: String?,
        authorId: String?,
        page: Int,
        pageSize: Int,
        maxCookingTime: Int?,
        tagIds: List<Int>?,
        ingredientIds: List<Int>?
    ): AppResult<PaginatedResponse<RecipeListItem>> = try {
        when (val response = api.searchRecipes(keyword, difficulty, authorId, page, pageSize, maxCookingTime, tagIds, ingredientIds)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun getRecipeDetail(id: String): AppResult<RecipeDetail> = try {
        when (val response = api.getRecipeDetail(id)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun createRecipe(request: CreateRecipeRequest): AppResult<RecipeDetail> = try {
        when (val response = api.createRecipe(request)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun updateRecipe(
        id: String,
        request: CreateRecipeRequest
    ): AppResult<RecipeDetail> = try {
        when (val response = api.updateRecipe(id, request)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun deleteRecipe(id: String): AppResult<Boolean> = try {
        when (val response = api.deleteRecipe(id)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun getRandomRecipes(count: Int): AppResult<List<RecipeListItem>> = try {
        when (val response = api.getRandomRecipes(count)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }
}
