package com.culino.feature.tool.presentation.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.core.common.AppResult
import com.culino.feature.tool.data.CreateShoppingItemRequest
import com.culino.feature.tool.data.ShoppingListDetail
import com.culino.feature.tool.data.ToolRepository
import com.culino.feature.tool.data.UpdateShoppingItemRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ShoppingListDetailState {
    data object Loading : ShoppingListDetailState
    data class Success(val detail: ShoppingListDetail) : ShoppingListDetailState
    data class Error(val message: String) : ShoppingListDetailState
}

class ShoppingListDetailViewModel(
    private val repository: ToolRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ShoppingListDetailState>(ShoppingListDetailState.Loading)
    val state: StateFlow<ShoppingListDetailState> = _state.asStateFlow()

    fun loadDetail(listId: String) {
        viewModelScope.launch {
            _state.value = ShoppingListDetailState.Loading
            when (val result = repository.getShoppingListDetail(listId)) {
                is AppResult.Success -> {
                    _state.value = ShoppingListDetailState.Success(result.data)
                }
                is AppResult.Error -> {
                    _state.value = ShoppingListDetailState.Error(result.message)
                }
            }
        }
    }

    fun addItem(listId: String, name: String, amount: String?) {
        viewModelScope.launch {
            val request = CreateShoppingItemRequest(name = name, amount = amount)
            when (repository.addShoppingItem(listId, request)) {
                is AppResult.Success -> loadDetail(listId)
                is AppResult.Error -> {
                    // 可以显示错误提示
                }
            }
        }
    }

    fun toggleItem(listId: String, itemId: Int, isChecked: Boolean) {
        viewModelScope.launch {
            val request = UpdateShoppingItemRequest(isChecked = isChecked)
            when (repository.updateShoppingItem(listId, itemId, request)) {
                is AppResult.Success -> loadDetail(listId)
                is AppResult.Error -> {
                    // 可以显示错误提示
                }
            }
        }
    }

    fun deleteItem(listId: String, itemId: Int) {
        viewModelScope.launch {
            when (repository.deleteShoppingItem(listId, itemId)) {
                is AppResult.Success -> loadDetail(listId)
                is AppResult.Error -> {
                    // 可以显示错误提示
                }
            }
        }
    }
}
