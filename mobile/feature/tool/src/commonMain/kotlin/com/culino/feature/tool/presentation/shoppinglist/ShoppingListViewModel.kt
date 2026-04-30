package com.culino.feature.tool.presentation.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.core.common.AppResult
import com.culino.feature.tool.data.CreateShoppingListRequest
import com.culino.feature.tool.data.ShoppingList
import com.culino.feature.tool.data.ToolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ShoppingListState {
    data object Loading : ShoppingListState
    data class Success(val lists: List<ShoppingList>) : ShoppingListState
    data class Error(val message: String) : ShoppingListState
}

class ShoppingListViewModel(
    private val repository: ToolRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ShoppingListState>(ShoppingListState.Loading)
    val state: StateFlow<ShoppingListState> = _state.asStateFlow()

    init {
        loadLists()
    }

    fun loadLists() {
        viewModelScope.launch {
            _state.value = ShoppingListState.Loading
            when (val result = repository.getShoppingLists()) {
                is AppResult.Success -> {
                    _state.value = ShoppingListState.Success(result.data)
                }
                is AppResult.Error -> {
                    _state.value = ShoppingListState.Error(result.message)
                }
            }
        }
    }

    fun createList(title: String) {
        viewModelScope.launch {
            when (repository.createShoppingList(CreateShoppingListRequest(title))) {
                is AppResult.Success -> loadLists()
                is AppResult.Error -> {
                    // 可以显示错误提示
                }
            }
        }
    }

    fun deleteList(id: String) {
        viewModelScope.launch {
            when (repository.deleteShoppingList(id)) {
                is AppResult.Success -> loadLists()
                is AppResult.Error -> {
                    // 可以显示错误提示
                }
            }
        }
    }
}
