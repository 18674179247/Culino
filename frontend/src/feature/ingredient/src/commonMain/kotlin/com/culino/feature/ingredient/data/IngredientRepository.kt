package com.culino.feature.ingredient.data

import com.culino.common.util.AppResult
import com.culino.framework.network.ApiResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IngredientRepository(private val api: IngredientApi) {

    private val mutex = Mutex()
    private var cachedIngredients: List<Ingredient>? = null
    private var cachedCategories: List<IngredientCategory>? = null
    private var cachedSeasonings: List<Seasoning>? = null
    private var cachedTags: List<Tag>? = null

    suspend fun getIngredients(): AppResult<List<Ingredient>> = mutex.withLock {
        cachedIngredients?.let { return AppResult.Success(it) }
        return when (val response = api.getIngredients()) {
            is ApiResponse.Success -> {
                cachedIngredients = response.data
                AppResult.Success(response.data)
            }
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    }

    suspend fun getCategories(): AppResult<List<IngredientCategory>> = mutex.withLock {
        cachedCategories?.let { return AppResult.Success(it) }
        return when (val response = api.getIngredientCategories()) {
            is ApiResponse.Success -> {
                cachedCategories = response.data
                AppResult.Success(response.data)
            }
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    }

    suspend fun getSeasonings(): AppResult<List<Seasoning>> = mutex.withLock {
        cachedSeasonings?.let { return AppResult.Success(it) }
        return when (val response = api.getSeasonings()) {
            is ApiResponse.Success -> {
                cachedSeasonings = response.data
                AppResult.Success(response.data)
            }
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    }

    suspend fun getTags(type: String? = null): AppResult<List<Tag>> {
        mutex.withLock {
            if (type == null) {
                cachedTags?.let { return AppResult.Success(it) }
            }
        }
        return when (val response = api.getTags(type)) {
            is ApiResponse.Success -> {
                if (type == null) {
                    mutex.withLock { cachedTags = response.data }
                }
                AppResult.Success(response.data)
            }
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    }

    suspend fun searchIngredients(query: String): List<Ingredient> {
        val all = when (val result = getIngredients()) {
            is AppResult.Success -> result.data
            is AppResult.Error -> return emptyList()
        }
        if (query.isBlank()) return all
        return all.filter { it.name.contains(query, ignoreCase = true) }
    }

    suspend fun searchSeasonings(query: String): List<Seasoning> {
        val all = when (val result = getSeasonings()) {
            is AppResult.Success -> result.data
            is AppResult.Error -> return emptyList()
        }
        if (query.isBlank()) return all
        return all.filter { it.name.contains(query, ignoreCase = true) }
    }

    suspend fun createTag(name: String, type: String): AppResult<Tag> {
        return when (val response = api.createTag(CreateTagRequest(name = name, type = type))) {
            is ApiResponse.Success -> {
                cachedTags = null
                AppResult.Success(response.data)
            }
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    }

    fun invalidateCache() {
        cachedIngredients = null
        cachedCategories = null
        cachedSeasonings = null
        cachedTags = null
    }
}
