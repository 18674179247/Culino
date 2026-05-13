package com.culino.feature.tool.data

import com.culino.common.util.AppResult
import com.culino.framework.network.safeApiCall

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

    override suspend fun getShoppingLists(): AppResult<List<ShoppingList>> =
        safeApiCall { api.getShoppingLists() }

    override suspend fun createShoppingList(request: CreateShoppingListRequest): AppResult<ShoppingList> =
        safeApiCall { api.createShoppingList(request) }

    override suspend fun getShoppingListDetail(id: String): AppResult<ShoppingListDetail> =
        safeApiCall { api.getShoppingListDetail(id) }

    override suspend fun deleteShoppingList(id: String): AppResult<Boolean> =
        safeApiCall { api.deleteShoppingList(id) }

    override suspend fun addShoppingItem(
        listId: String,
        request: CreateShoppingItemRequest
    ): AppResult<ShoppingListItem> = safeApiCall { api.addShoppingItem(listId, request) }

    override suspend fun updateShoppingItem(
        listId: String,
        itemId: Int,
        request: UpdateShoppingItemRequest
    ): AppResult<ShoppingListItem> = safeApiCall { api.updateShoppingItem(listId, itemId, request) }

    override suspend fun deleteShoppingItem(listId: String, itemId: Int): AppResult<Boolean> =
        safeApiCall { api.deleteShoppingItem(listId, itemId) }

    override suspend fun batchAddItems(
        listId: String,
        items: List<CreateShoppingItemRequest>
    ): AppResult<List<ShoppingListItem>> =
        safeApiCall { api.batchAddItems(listId, BatchAddItemsRequest(items)) }

    override suspend fun parseShoppingText(text: String): AppResult<ParseShoppingTextResponse> =
        safeApiCall { api.parseShoppingText(ParseShoppingTextRequest(text)) }

    override suspend fun getMealPlans(
        startDate: String?,
        endDate: String?
    ): AppResult<List<MealPlan>> = safeApiCall { api.getMealPlans(startDate, endDate) }

    override suspend fun createMealPlan(request: CreateMealPlanRequest): AppResult<MealPlan> =
        safeApiCall { api.createMealPlan(request) }

    override suspend fun updateMealPlan(
        id: String,
        request: UpdateMealPlanRequest
    ): AppResult<MealPlan> = safeApiCall { api.updateMealPlan(id, request) }

    override suspend fun deleteMealPlan(id: String): AppResult<Boolean> =
        safeApiCall { api.deleteMealPlan(id) }
}
