package com.menu.feature.social.data

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Favorite(
    @SerialName("user_id") val userId: String,
    @SerialName("recipe_id") val recipeId: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("recipe_title") val recipeTitle: String? = null
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
