package com.culino.feature.tool.data

import com.culino.core.network.ApiClient
import com.culino.core.network.ApiResponse

interface ToolApi {
    // Shopping Lists
    suspend fun getShoppingLists(): ApiResponse<List<ShoppingList>>
    suspend fun createShoppingList(request: CreateShoppingListRequest): ApiResponse<ShoppingList>
    suspend fun getShoppingListDetail(id: String): ApiResponse<ShoppingListDetail>
    suspend fun deleteShoppingList(id: String): ApiResponse<Boolean>

    // Shopping List Items
    suspend fun addShoppingItem(listId: String, request: CreateShoppingItemRequest): ApiResponse<ShoppingListItem>
    suspend fun updateShoppingItem(listId: String, itemId: Int, request: UpdateShoppingItemRequest): ApiResponse<ShoppingListItem>
    suspend fun deleteShoppingItem(listId: String, itemId: Int): ApiResponse<Boolean>
    suspend fun batchAddItems(listId: String, request: BatchAddItemsRequest): ApiResponse<List<ShoppingListItem>>

    // AI
    suspend fun parseShoppingText(request: ParseShoppingTextRequest): ApiResponse<ParseShoppingTextResponse>

    // Meal Plans
    suspend fun getMealPlans(startDate: String? = null, endDate: String? = null): ApiResponse<List<MealPlan>>
    suspend fun createMealPlan(request: CreateMealPlanRequest): ApiResponse<MealPlan>
    suspend fun updateMealPlan(id: String, request: UpdateMealPlanRequest): ApiResponse<MealPlan>
    suspend fun deleteMealPlan(id: String): ApiResponse<Boolean>
}

class ToolApiImpl(private val client: ApiClient) : ToolApi {

    // region Shopping Lists

    override suspend fun getShoppingLists(): ApiResponse<List<ShoppingList>> {
        return client.get("tool/shopping-lists")
    }

    override suspend fun createShoppingList(request: CreateShoppingListRequest): ApiResponse<ShoppingList> {
        return client.post("tool/shopping-lists", request)
    }

    override suspend fun getShoppingListDetail(id: String): ApiResponse<ShoppingListDetail> {
        return client.get("tool/shopping-lists/$id")
    }

    override suspend fun deleteShoppingList(id: String): ApiResponse<Boolean> {
        return client.delete("tool/shopping-lists/$id")
    }

    // endregion

    // region Shopping List Items

    override suspend fun addShoppingItem(
        listId: String,
        request: CreateShoppingItemRequest
    ): ApiResponse<ShoppingListItem> {
        return client.post("tool/shopping-lists/$listId/items", request)
    }

    override suspend fun updateShoppingItem(
        listId: String,
        itemId: Int,
        request: UpdateShoppingItemRequest
    ): ApiResponse<ShoppingListItem> {
        return client.put("tool/shopping-lists/$listId/items/$itemId", request)
    }

    override suspend fun deleteShoppingItem(listId: String, itemId: Int): ApiResponse<Boolean> {
        return client.delete("tool/shopping-lists/$listId/items/$itemId")
    }

    override suspend fun batchAddItems(
        listId: String,
        request: BatchAddItemsRequest
    ): ApiResponse<List<ShoppingListItem>> {
        return client.post("tool/shopping-lists/$listId/items/batch", request)
    }

    override suspend fun parseShoppingText(request: ParseShoppingTextRequest): ApiResponse<ParseShoppingTextResponse> {
        return client.post("ai/shopping-list/parse", request)
    }

    // endregion

    // region Meal Plans

    override suspend fun getMealPlans(
        startDate: String?,
        endDate: String?
    ): ApiResponse<List<MealPlan>> {
        val params = buildMap {
            startDate?.let { put("start_date", it) }
            endDate?.let { put("end_date", it) }
        }
        return if (params.isEmpty()) {
            client.get("tool/meal-plans")
        } else {
            client.get("tool/meal-plans", params)
        }
    }

    override suspend fun createMealPlan(request: CreateMealPlanRequest): ApiResponse<MealPlan> {
        return client.post("tool/meal-plans", request)
    }

    override suspend fun updateMealPlan(
        id: String,
        request: UpdateMealPlanRequest
    ): ApiResponse<MealPlan> {
        return client.put("tool/meal-plans/$id", request)
    }

    override suspend fun deleteMealPlan(id: String): ApiResponse<Boolean> {
        return client.delete("tool/meal-plans/$id")
    }

    // endregion
}
