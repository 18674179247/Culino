package com.culino.feature.social.data

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Favorite(
    @SerialName("user_id") val userId: String,
    @SerialName("recipe_id") val recipeId: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("recipe_title") val recipeTitle: String? = null,
    @SerialName("cover_image") val coverImage: String? = null,
    val difficulty: Int? = null,
    @SerialName("cooking_time") val cookingTime: Int? = null,
    val servings: Int? = null
)

@Serializable
data class CookingLog(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("recipe_id") val recipeId: String,
    @SerialName("recipe_title") val recipeTitle: String?,
    val rating: Int?,
    val notes: String?,
    @SerialName("cooked_at") val cookedAt: Instant,
    @SerialName("created_at") val createdAt: Instant
)

@Serializable
data class CreateCookingLogRequest(
    @SerialName("recipe_id") val recipeId: String,
    val rating: Int?,
    val notes: String?
)

@Serializable
data class UpdateCookingLogRequest(
    val rating: Int?,
    val notes: String?
)

@Serializable
data class RecipeComment(
    val id: String,
    @SerialName("recipe_id") val recipeId: String,
    @SerialName("user_id") val userId: String,
    val username: String,
    val nickname: String? = null,
    val avatar: String? = null,
    val content: String,
    @SerialName("created_at") val createdAt: Instant? = null
)

@Serializable
data class CreateCommentRequest(
    @SerialName("recipe_id") val recipeId: String,
    val content: String
)

@Serializable
data class CommentListResponse(
    val data: List<RecipeComment>,
    val total: Long,
    val page: Int,
    @SerialName("page_size") val pageSize: Int
)
