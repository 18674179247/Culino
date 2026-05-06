package com.culino.feature.ingredient.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val id: Int,
    val name: String,
    @SerialName("category_id") val categoryId: Int? = null,
    val unit: String? = null,
    val image: String? = null
)

@Serializable
data class IngredientCategory(
    val id: Int,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int? = null
)

@Serializable
data class Seasoning(
    val id: Int,
    val name: String,
    val unit: String? = null,
    val image: String? = null
)

@Serializable
data class Tag(
    val id: Int,
    val name: String,
    val type: String,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null
)
