package com.culino.feature.recipe.data

import com.culino.core.network.ApiClient
import com.culino.core.network.ApiResponse

interface RecipeApi {
    suspend fun searchRecipes(
        keyword: String? = null,
        difficulty: String? = null,
        authorId: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
        maxCookingTime: Int? = null,
        tagIds: List<Int>? = null,
        ingredientIds: List<Int>? = null
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
        authorId: String?,
        page: Int,
        pageSize: Int,
        maxCookingTime: Int?,
        tagIds: List<Int>?,
        ingredientIds: List<Int>?
    ): ApiResponse<PaginatedResponse<RecipeListItem>> {
        val params = buildMap {
            keyword?.let { put("keyword", it) }
            difficulty?.let { put("difficulty", it) }
            authorId?.let { put("author_id", it) }
            put("page", page.toString())
            put("page_size", pageSize.toString())
            maxCookingTime?.let { put("max_cooking_time", it.toString()) }
            tagIds?.takeIf { it.isNotEmpty() }?.let { put("tag_ids", it.joinToString(",")) }
            ingredientIds?.takeIf { it.isNotEmpty() }?.let { put("ingredient_ids", it.joinToString(",")) }
        }
        return client.get("recipe/search", params)
    }

    override suspend fun getRecipeDetail(id: String): ApiResponse<RecipeDetail> {
        return client.get("recipe/$id")
    }

    override suspend fun createRecipe(request: CreateRecipeRequest): ApiResponse<RecipeDetail> {
        return client.post("recipe", request)
    }

    override suspend fun updateRecipe(
        id: String,
        request: CreateRecipeRequest
    ): ApiResponse<RecipeDetail> {
        return client.put("recipe/$id", request)
    }

    override suspend fun deleteRecipe(id: String): ApiResponse<Boolean> {
        return client.delete("recipe/$id")
    }

    override suspend fun getRandomRecipes(count: Int): ApiResponse<List<RecipeListItem>> {
        return client.get("recipe/random", mapOf("count" to count.toString()))
    }
}
