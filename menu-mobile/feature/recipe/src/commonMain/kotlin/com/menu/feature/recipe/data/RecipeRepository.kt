package com.menu.feature.recipe.data

import com.menu.core.common.Result

interface RecipeRepository {
    suspend fun searchRecipes(
        keyword: String? = null,
        difficulty: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<PaginatedResponse<RecipeListItem>>

    suspend fun getRecipeDetail(id: String): Result<RecipeDetail>

    suspend fun createRecipe(request: CreateRecipeRequest): Result<RecipeDetail>

    suspend fun updateRecipe(id: String, request: CreateRecipeRequest): Result<RecipeDetail>

    suspend fun deleteRecipe(id: String): Result<Boolean>

    suspend fun getRandomRecipes(count: Int = 5): Result<List<RecipeListItem>>
}

class RecipeRepositoryImpl(private val api: RecipeApi) : RecipeRepository {
    override suspend fun searchRecipes(
        keyword: String?,
        difficulty: String?,
        page: Int,
        pageSize: Int
    ): Result<PaginatedResponse<RecipeListItem>> = try {
        when (val response = api.searchRecipes(keyword, difficulty, page, pageSize)) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun getRecipeDetail(id: String): Result<RecipeDetail> = try {
        when (val response = api.getRecipeDetail(id)) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun createRecipe(request: CreateRecipeRequest): Result<RecipeDetail> = try {
        when (val response = api.createRecipe(request)) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun updateRecipe(
        id: String,
        request: CreateRecipeRequest
    ): Result<RecipeDetail> = try {
        when (val response = api.updateRecipe(id, request)) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun deleteRecipe(id: String): Result<Boolean> = try {
        when (val response = api.deleteRecipe(id)) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }

    override suspend fun getRandomRecipes(count: Int): Result<List<RecipeListItem>> = try {
        when (val response = api.getRandomRecipes(count)) {
            is ApiResponse.Success -> Result.Success(response.data)
            is ApiResponse.Error -> Result.Error(Exception(response.message))
        }
    } catch (e: Exception) {
        Result.Error(e)
    }
}
