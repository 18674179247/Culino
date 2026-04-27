package com.menu.core.model

import kotlinx.serialization.Serializable

/**
 * 菜谱营养信息
 */
@Serializable
data class RecipeNutrition(
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbohydrate: Double? = null,
    val fiber: Double? = null,
    val sodium: Double? = null,
    val healthScore: Int? = null,
    val healthTags: List<String>? = null,
    val suitableFor: List<String>? = null,
    val analysisText: String? = null
)

/**
 * 推荐菜谱项
 */
@Serializable
data class RecommendationItem(
    val recipeId: String,
    val title: String,
    val coverImage: String? = null,
    val score: Double,
    val reason: String,
    val recommendationType: String
)

/**
 * 用户偏好画像
 */
@Serializable
data class UserPreference(
    val userId: String,
    val favoriteCuisines: Map<String, Double>? = null,
    val favoriteTastes: Map<String, Double>? = null,
    val favoriteIngredients: Map<String, Double>? = null,
    val favoriteTags: Map<String, Double>? = null,
    val dietaryRestrictions: List<String>? = null,
    val healthGoals: List<String>? = null,
    val avgCookingTime: Int? = null,
    val difficultyPreference: Int? = null,
    val totalFavorites: Int? = null,
    val totalCookingLogs: Int? = null,
    val avgRating: Double? = null
)

/**
 * 行为日志请求
 */
@Serializable
data class BehaviorLogRequest(
    val recipeId: String,
    val actionType: String,
    val actionValue: Map<String, String>? = null
)
