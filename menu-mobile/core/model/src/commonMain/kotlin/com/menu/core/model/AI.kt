package com.menu.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecipeNutrition(
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbohydrate: Double? = null,
    val fiber: Double? = null,
    val sodium: Double? = null,
    @SerialName("health_score") val healthScore: Int? = null,
    @SerialName("health_tags") val healthTags: List<String>? = null,
    @SerialName("suitable_for") val suitableFor: List<String>? = null,
    @SerialName("analysis_text") val analysisText: String? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("traffic_light") val trafficLight: Map<String, String>? = null,
    @SerialName("overall_rating") val overallRating: String? = null,
    val summary: String? = null,
    val cautions: List<String>? = null
)

@Serializable
data class RecommendationItem(
    val recipeId: String,
    val title: String,
    val coverImage: String? = null,
    val score: Double,
    val reason: String,
    val recommendationType: String
)

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

@Serializable
data class BehaviorLogRequest(
    val recipeId: String,
    val actionType: String,
    val actionValue: Map<String, String>? = null
)

@Serializable
data class RecognizeRecipeRequest(
    @SerialName("image_url") val imageUrl: String,
    @SerialName("existing_title") val existingTitle: String? = null
)

@Serializable
data class RecognizedIngredient(
    val name: String,
    val amount: String
)

@Serializable
data class RecognizeRecipeResponse(
    val title: String,
    val description: String? = null,
    val difficulty: Int? = null,
    @SerialName("cooking_time") val cookingTime: Int? = null,
    val servings: Int? = null,
    val ingredients: List<RecognizedIngredient> = emptyList(),
    val seasonings: List<RecognizedIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val confidence: Double = 0.0
)
