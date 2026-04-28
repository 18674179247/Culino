package com.menu.feature.recipe.data

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
data class RecipeDetail(
    val recipe: Recipe,
    val ingredients: List<RecipeIngredient>,
    val seasonings: List<RecipeSeasoning>,
    val steps: List<RecipeStep>,
    val tags: List<RecipeTag>,
    val nutrition: RecipeNutrition? = null
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
    @SerialName("ingredient_id") val ingredientId: Int,
    @SerialName("ingredient_name") val ingredientName: String,
    val amount: String,
    @SerialName("sort_order") val sortOrder: Int
)

@Serializable
data class RecipeSeasoning(
    val id: Int,
    @SerialName("seasoning_id") val seasoningId: Int,
    @SerialName("seasoning_name") val seasoningName: String,
    val amount: String,
    @SerialName("sort_order") val sortOrder: Int
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
    val id: Int,
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
    @SerialName("analysis_text") val analysisText: String?
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
    @SerialName("ingredient_id") val ingredientId: Int,
    val amount: String,
    @SerialName("sort_order") val sortOrder: Int
)

@Serializable
data class CreateRecipeSeasoning(
    @SerialName("seasoning_id") val seasoningId: Int,
    val amount: String,
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
