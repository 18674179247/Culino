package com.culino.common.api

import com.culino.common.util.AppResult
import com.culino.common.model.*
import com.culino.framework.network.ApiClient
import com.culino.framework.network.ApiResponse
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * AI 功能 API 服务
 */
class AiApiService(private val apiClient: ApiClient, private val baseUrl: String) {

    // ============================================
    // 营养分析
    // ============================================

    /**
     * 触发菜谱营养分析
     */
    suspend fun analyzeNutrition(recipeId: String): AppResult<ApiResponse<RecipeNutrition>> {
        return apiClient.safeRequest {
            url("$baseUrl/api/v1/ai/nutrition/analyze/$recipeId")
            method = HttpMethod.Post
        }
    }

    /**
     * 获取菜谱营养信息
     */
    suspend fun getNutrition(recipeId: String): AppResult<ApiResponse<RecipeNutrition>> {
        return apiClient.safeRequest {
            url("$baseUrl/api/v1/ai/nutrition/$recipeId")
            method = HttpMethod.Get
        }
    }

    // ============================================
    // 智能推荐
    // ============================================

    /**
     * 获取个性化推荐
     */
    suspend fun getPersonalizedRecommendations(
        limit: Int = 10
    ): AppResult<ApiResponse<List<RecommendationItem>>> {
        return apiClient.safeRequest {
            url("$baseUrl/api/v1/ai/recommend/personalized") {
                parameters.append("limit", limit.toString())
            }
            method = HttpMethod.Get
        }
    }

    /**
     * 获取相似菜谱推荐
     */
    suspend fun getSimilarRecommendations(
        recipeId: String,
        limit: Int = 10
    ): AppResult<ApiResponse<List<RecommendationItem>>> {
        return apiClient.safeRequest {
            url("$baseUrl/api/v1/ai/recommend/similar/$recipeId") {
                parameters.append("limit", limit.toString())
            }
            method = HttpMethod.Get
        }
    }

    /**
     * 获取热门推荐
     */
    suspend fun getTrendingRecommendations(
        limit: Int = 10
    ): AppResult<ApiResponse<List<RecommendationItem>>> {
        return apiClient.safeRequest {
            url("$baseUrl/api/v1/ai/recommend/trending") {
                parameters.append("limit", limit.toString())
            }
            method = HttpMethod.Get
        }
    }

    /**
     * 获取基于健康目标的推荐
     * @param goal 健康目标：减脂、增肌、保持健康
     */
    suspend fun getHealthGoalRecommendations(
        goal: String,
        limit: Int = 10
    ): AppResult<ApiResponse<List<RecommendationItem>>> {
        return apiClient.safeRequest {
            url("$baseUrl/api/v1/ai/recommend/health/$goal") {
                parameters.append("limit", limit.toString())
            }
            method = HttpMethod.Get
        }
    }

    // ============================================
    // 用户偏好
    // ============================================

    /**
     * 分析用户偏好
     */
    suspend fun analyzePreference(): AppResult<ApiResponse<UserPreference>> {
        return apiClient.safeRequest {
            url("$baseUrl/api/v1/ai/preference/analyze")
            method = HttpMethod.Post
        }
    }

    /**
     * 获取用户偏好画像
     */
    suspend fun getPreferenceProfile(): AppResult<ApiResponse<UserPreference>> {
        return apiClient.safeRequest {
            url("$baseUrl/api/v1/ai/preference/profile")
            method = HttpMethod.Get
        }
    }

    // ============================================
    // 行为日志
    // ============================================

    /**
     * 记录用户行为
     */
    suspend fun logBehavior(request: BehaviorLogRequest): AppResult<ApiResponse<Boolean>> {
        return apiClient.safeRequest {
            url("$baseUrl/api/v1/ai/behavior/log")
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /**
     * 记录浏览行为
     */
    suspend fun logView(recipeId: String) {
        logBehavior(BehaviorLogRequest(recipeId, "view"))
    }

    /**
     * 记录收藏行为
     */
    suspend fun logFavorite(recipeId: String) {
        logBehavior(BehaviorLogRequest(recipeId, "favorite"))
    }

    /**
     * 记录取消收藏行为
     */
    suspend fun logUnfavorite(recipeId: String) {
        logBehavior(BehaviorLogRequest(recipeId, "unfavorite"))
    }

    /**
     * 记录烹饪行为
     */
    suspend fun logCook(recipeId: String, rating: Int? = null) {
        val actionValue = rating?.let { mapOf("rating" to it.toString()) }
        logBehavior(BehaviorLogRequest(recipeId, "cook", actionValue))
    }

    suspend fun recognizeRecipe(
        imageUrl: String,
        existingTitle: String? = null
    ): ApiResponse<RecognizeRecipeResponse> {
        return apiClient.post("ai/recipe/recognize", RecognizeRecipeRequest(imageUrl, existingTitle))
    }
}
