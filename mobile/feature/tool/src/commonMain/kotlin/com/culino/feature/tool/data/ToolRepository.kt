package com.culino.feature.tool.data

import com.culino.core.common.AppResult
import com.culino.core.network.ApiResponse

interface ToolRepository {
    // Shopping Lists
    suspend fun getShoppingLists(): AppResult<List<ShoppingList>>
    suspend fun createShoppingList(request: CreateShoppingListRequest): AppResult<ShoppingList>
    suspend fun getShoppingListDetail(id: String): AppResult<ShoppingListDetail>
    suspend fun deleteShoppingList(id: String): AppResult<Boolean>

    // Shopping List Items
    suspend fun addShoppingItem(listId: String, request: CreateShoppingItemRequest): AppResult<ShoppingListItem>
    suspend fun updateShoppingItem(listId: String, itemId: Int, request: UpdateShoppingItemRequest): AppResult<ShoppingListItem>
    suspend fun deleteShoppingItem(listId: String, itemId: Int): AppResult<Boolean>
    suspend fun batchAddItems(listId: String, items: List<CreateShoppingItemRequest>): AppResult<List<ShoppingListItem>>
    suspend fun parseShoppingText(text: String): AppResult<ParseShoppingTextResponse>

    // Meal Plans
    suspend fun getMealPlans(startDate: String? = null, endDate: String? = null): AppResult<List<MealPlan>>
    suspend fun createMealPlan(request: CreateMealPlanRequest): AppResult<MealPlan>
    suspend fun updateMealPlan(id: String, request: UpdateMealPlanRequest): AppResult<MealPlan>
    suspend fun deleteMealPlan(id: String): AppResult<Boolean>
}

class ToolRepositoryImpl(private val api: ToolApi) : ToolRepository {

    override suspend fun getShoppingLists(): AppResult<List<ShoppingList>> = try {
        when (val response = api.getShoppingLists()) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun createShoppingList(request: CreateShoppingListRequest): AppResult<ShoppingList> = try {
        when (val response = api.createShoppingList(request)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun getShoppingListDetail(id: String): AppResult<ShoppingListDetail> = try {
        when (val response = api.getShoppingListDetail(id)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun deleteShoppingList(id: String): AppResult<Boolean> = try {
        when (val response = api.deleteShoppingList(id)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun addShoppingItem(
        listId: String,
        request: CreateShoppingItemRequest
    ): AppResult<ShoppingListItem> = try {
        when (val response = api.addShoppingItem(listId, request)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun updateShoppingItem(
        listId: String,
        itemId: Int,
        request: UpdateShoppingItemRequest
    ): AppResult<ShoppingListItem> = try {
        when (val response = api.updateShoppingItem(listId, itemId, request)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun deleteShoppingItem(listId: String, itemId: Int): AppResult<Boolean> = try {
        when (val response = api.deleteShoppingItem(listId, itemId)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun batchAddItems(
        listId: String,
        items: List<CreateShoppingItemRequest>
    ): AppResult<List<ShoppingListItem>> = try {
        when (val response = api.batchAddItems(listId, BatchAddItemsRequest(items))) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun parseShoppingText(text: String): AppResult<ParseShoppingTextResponse> = try {
        when (val response = api.parseShoppingText(ParseShoppingTextRequest(text))) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun getMealPlans(
        startDate: String?,
        endDate: String?
    ): AppResult<List<MealPlan>> = try {
        when (val response = api.getMealPlans(startDate, endDate)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun createMealPlan(request: CreateMealPlanRequest): AppResult<MealPlan> = try {
        when (val response = api.createMealPlan(request)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun updateMealPlan(
        id: String,
        request: UpdateMealPlanRequest
    ): AppResult<MealPlan> = try {
        when (val response = api.updateMealPlan(id, request)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }

    override suspend fun deleteMealPlan(id: String): AppResult<Boolean> = try {
        when (val response = api.deleteMealPlan(id)) {
            is ApiResponse.Success -> AppResult.Success(response.data)
            is ApiResponse.Error -> AppResult.Error(response.message)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error", e)
    }
}
