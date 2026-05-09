package com.culino.feature.recipe.data

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecipeListItem(
    val id: String,
    val title: String,
    val description: String?,
    @SerialName("cover_image") val coverImage: String?,
    val difficulty: Int,
    @SerialName("cooking_time") val cookingTime: Int,
    @SerialName("servings") val servings: Int,
    @SerialName("author_id") val authorId: String,
    @SerialName("created_at") val createdAt: Instant
)

@Serializable
data class AuthorInfo(
    val id: String,
    val username: String,
    val nickname: String? = null,
    val avatar: String? = null
)

@Serializable
data class RecipeDetail(
    val recipe: Recipe,
    val ingredients: List<RecipeIngredient>,
    val seasonings: List<RecipeSeasoning>,
    val steps: List<RecipeStep>,
    val tags: List<RecipeTag>,
    val nutrition: RecipeNutrition? = null,
    val author: AuthorInfo? = null,
    @SerialName("like_count") val likeCount: Long? = null,
    @SerialName("comment_count") val commentCount: Long? = null
)

@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val description: String?,
    @SerialName("cover_image") val coverImage: String?,
    val difficulty: Int?,
    @SerialName("cooking_time") val cookingTime: Int?,
    val servings: Int?,
    @SerialName("author_id") val authorId: String?,
    @SerialName("created_at") val createdAt: Instant?,
    @SerialName("updated_at") val updatedAt: Instant?
)

@Serializable
data class RecipeIngredient(
    val id: Int,
    @SerialName("ingredient_id") val ingredientId: Int? = null,
    @SerialName("ingredient_name") val ingredientName: String = "",
    val amount: String? = null,
    val unit: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null
)

@Serializable
data class RecipeSeasoning(
    val id: Int,
    @SerialName("seasoning_id") val seasoningId: Int? = null,
    @SerialName("seasoning_name") val seasoningName: String = "",
    val amount: String? = null,
    val unit: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null
)

@Serializable
data class RecipeStep(
    val id: Int,
    @SerialName("step_number") val stepNumber: Int,
    val content: String,
    val image: String?,
    val duration: Int?
)

@Serializable
data class RecipeTag(
    @SerialName("recipe_id") val recipeId: String,
    @SerialName("tag_id") val tagId: Int,
    @SerialName("tag_name") val tagName: String
)

@Serializable
data class RecipeNutrition(
    val calories: Double?,
    val protein: Double?,
    val fat: Double?,
    val carbohydrate: Double?,
    val fiber: Double?,
    val sodium: Double?,
    @SerialName("health_score") val healthScore: Int?,
    @SerialName("health_tags") val healthTags: List<String>?,
    @SerialName("suitable_for") val suitableFor: List<String>?,
    @SerialName("analysis_text") val analysisText: String?,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("traffic_light") val trafficLight: Map<String, String>? = null,
    @SerialName("overall_rating") val overallRating: String? = null,
    val summary: String? = null,
    val cautions: List<String>? = null
)

@Serializable
data class CreateRecipeRequest(
    val title: String,
    val description: String?,
    @SerialName("cover_image") val coverImage: String?,
    val difficulty: Int?,
    @SerialName("cooking_time") val cookingTime: Int?,
    @SerialName("prep_time") val prepTime: Int?,
    val servings: Int?,
    val ingredients: List<CreateRecipeIngredient>?,
    val seasonings: List<CreateRecipeSeasoning>?,
    val steps: List<CreateRecipeStep>?,
    @SerialName("tag_ids") val tagIds: List<Int>?
)

@Serializable
data class CreateRecipeIngredient(
    @SerialName("ingredient_id") val ingredientId: Int? = null,
    val name: String? = null,
    val amount: String? = null,
    @SerialName("sort_order") val sortOrder: Int
)

@Serializable
data class CreateRecipeSeasoning(
    @SerialName("seasoning_id") val seasoningId: Int? = null,
    val name: String? = null,
    val amount: String? = null,
    @SerialName("sort_order") val sortOrder: Int
)

@Serializable
data class CreateRecipeStep(
    @SerialName("step_number") val stepNumber: Int,
    val content: String,
    val image: String?,
    val duration: Int?
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val total: Long,
    val page: Int,
    @SerialName("page_size") val pageSize: Int
)
