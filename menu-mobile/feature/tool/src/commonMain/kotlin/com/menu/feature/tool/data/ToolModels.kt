package com.menu.feature.tool.data

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// region Shopping List

@Serializable
data class ShoppingList(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val status: String,
    @SerialName("created_at") val createdAt: Instant
)

@Serializable
data class ShoppingListItem(
    val id: Int,
    @SerialName("list_id") val listId: String,
    val name: String,
    val amount: String? = null,
    @SerialName("is_checked") val isChecked: Boolean,
    @SerialName("sort_order") val sortOrder: Int
)

@Serializable
data class ShoppingListDetail(
    val list: ShoppingList,
    val items: List<ShoppingListItem>
)

@Serializable
data class CreateShoppingListRequest(
    val title: String
)

@Serializable
data class CreateShoppingItemRequest(
    val name: String,
    val amount: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null
)

@Serializable
data class UpdateShoppingItemRequest(
    val name: String? = null,
    val amount: String? = null,
    @SerialName("is_checked") val isChecked: Boolean? = null,
    @SerialName("sort_order") val sortOrder: Int? = null
)

// endregion

// region Meal Plan

@Serializable
data class MealPlan(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("recipe_id") val recipeId: String,
    @SerialName("plan_date") val planDate: String,
    @SerialName("meal_type") val mealType: Int,
    val note: String? = null
)

@Serializable
data class CreateMealPlanRequest(
    @SerialName("recipe_id") val recipeId: String,
    @SerialName("plan_date") val planDate: String,
    @SerialName("meal_type") val mealType: Int,
    val note: String? = null
)

@Serializable
data class UpdateMealPlanRequest(
    @SerialName("recipe_id") val recipeId: String? = null,
    @SerialName("plan_date") val planDate: String? = null,
    @SerialName("meal_type") val mealType: Int? = null,
    val note: String? = null
)

// endregion
