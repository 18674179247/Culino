package com.culino.feature.ingredient.data

import com.culino.core.network.ApiClient
import com.culino.core.network.ApiResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTagRequest(
    val name: String,
    val type: String,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null
)

interface IngredientApi {
    suspend fun getIngredients(): ApiResponse<List<Ingredient>>
    suspend fun getIngredientCategories(): ApiResponse<List<IngredientCategory>>
    suspend fun getSeasonings(): ApiResponse<List<Seasoning>>
    suspend fun getTags(type: String? = null): ApiResponse<List<Tag>>
    suspend fun createTag(request: CreateTagRequest): ApiResponse<Tag>
}

class IngredientApiImpl(private val client: ApiClient) : IngredientApi {

    override suspend fun getIngredients(): ApiResponse<List<Ingredient>> {
        return client.get("ingredient/ingredients")
    }

    override suspend fun getIngredientCategories(): ApiResponse<List<IngredientCategory>> {
        return client.get("ingredient/ingredient-categories")
    }

    override suspend fun getSeasonings(): ApiResponse<List<Seasoning>> {
        return client.get("ingredient/seasonings")
    }

    override suspend fun getTags(type: String?): ApiResponse<List<Tag>> {
        return if (type != null) {
            client.get("ingredient/tags", mapOf("type" to type))
        } else {
            client.get("ingredient/tags")
        }
    }

    override suspend fun createTag(request: CreateTagRequest): ApiResponse<Tag> {
        return client.post("ingredient/tags", request)
    }
}
