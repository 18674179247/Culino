package com.culino.feature.recipe.data

import com.culino.common.util.AppResult
import com.culino.framework.network.safeApiCall

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
    ): AppResult<PaginatedResponse<RecipeListItem>> = safeApiCall {
        api.searchRecipes(keyword, difficulty, authorId, page, pageSize, maxCookingTime, tagIds, ingredientIds)
    }

    override suspend fun getRecipeDetail(id: String): AppResult<RecipeDetail> =
        safeApiCall { api.getRecipeDetail(id) }

    override suspend fun createRecipe(request: CreateRecipeRequest): AppResult<RecipeDetail> =
        safeApiCall { api.createRecipe(request) }

    override suspend fun updateRecipe(
        id: String,
        request: CreateRecipeRequest
    ): AppResult<RecipeDetail> = safeApiCall { api.updateRecipe(id, request) }

    override suspend fun deleteRecipe(id: String): AppResult<Boolean> =
        safeApiCall { api.deleteRecipe(id) }

    override suspend fun getRandomRecipes(count: Int): AppResult<List<RecipeListItem>> =
        safeApiCall { api.getRandomRecipes(count) }
}
