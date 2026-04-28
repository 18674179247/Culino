package com.menu.feature.recipe.data

import com.menu.core.network.ApiClient
import com.menu.core.network.ApiResponse

interface RecipeApi {
    suspend fun searchRecipes(
        keyword: String? = null,
        difficulty: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): ApiResponse<PaginatedResponse<RecipeListItem>>

    suspend fun getRecipeDetail(id: String): ApiResponse<RecipeDetail>

    suspend fun createRecipe(request: CreateRecipeRequest): ApiResponse<RecipeDetail>

    suspend fun updateRecipe(id: String, request: CreateRecipeRequest): ApiResponse<RecipeDetail>

    suspend fun deleteRecipe(id: String): ApiResponse<Boolean>

    suspend fun getRandomRecipes(count: Int = 5): ApiResponse<List<RecipeListItem>>
}

class RecipeApiImpl(private val client: ApiClient) : RecipeApi {
    override suspend fun searchRecipes(
        keyword: String?,
        difficulty: String?,
        page: Int,
        pageSize: Int
    ): ApiResponse<PaginatedResponse<RecipeListItem>> {
        val params = buildMap {
            keyword?.let { put("keyword", it) }
            difficulty?.let { put("difficulty", it) }
            put("page", page.toString())
            put("page_size", pageSize.toString())
        }
        return client.get("/recipe/search", params)
    }

    override suspend fun getRecipeDetail(id: String): ApiResponse<RecipeDetail> {
        return client.get("/recipe/$id")
    }

    override suspend fun createRecipe(request: CreateRecipeRequest): ApiResponse<RecipeDetail> {
        return client.post("/recipe", request)
    }

    override suspend fun updateRecipe(
        id: String,
        request: CreateRecipeRequest
    ): ApiResponse<RecipeDetail> {
        return client.put("/recipe/$id", request)
    }

    override suspend fun deleteRecipe(id: String): ApiResponse<Boolean> {
        return client.delete("/recipe/$id")
    }

    override suspend fun getRandomRecipes(count: Int): ApiResponse<List<RecipeListItem>> {
        return client.get("/recipe/random", mapOf("count" to count.toString()))
    }
}
