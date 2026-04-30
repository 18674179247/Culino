package com.culino.feature.tool.presentation.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.core.common.AppResult
import com.culino.feature.tool.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ShoppingListDetailState {
    data object Loading : ShoppingListDetailState
    data class Success(val detail: ShoppingListDetail) : ShoppingListDetailState
    data class Error(val message: String) : ShoppingListDetailState
}

sealed interface AiParseState {
    data object Idle : AiParseState
    data object Loading : AiParseState
    data class Success(val items: List<ParsedShoppingItem>) : AiParseState
    data class Error(val message: String) : AiParseState
}

class ShoppingListDetailViewModel(
    private val repository: ToolRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ShoppingListDetailState>(ShoppingListDetailState.Loading)
    val state: StateFlow<ShoppingListDetailState> = _state.asStateFlow()

    private val _aiState = MutableStateFlow<AiParseState>(AiParseState.Idle)
    val aiState: StateFlow<AiParseState> = _aiState.asStateFlow()

    fun loadDetail(listId: String) {
        viewModelScope.launch {
            if (_state.value !is ShoppingListDetailState.Success) {
                _state.value = ShoppingListDetailState.Loading
            }
            when (val result = repository.getShoppingListDetail(listId)) {
                is AppResult.Success -> _state.value = ShoppingListDetailState.Success(result.data)
                is AppResult.Error -> _state.value = ShoppingListDetailState.Error(result.message)
            }
        }
    }

    fun addItem(listId: String, name: String, amount: String?) {
        viewModelScope.launch {
            val request = CreateShoppingItemRequest(name = name, amount = amount)
            when (repository.addShoppingItem(listId, request)) {
                is AppResult.Success -> loadDetail(listId)
                is AppResult.Error -> {}
            }
        }
    }

    fun toggleItem(listId: String, itemId: Int, isChecked: Boolean) {
        viewModelScope.launch {
            val request = UpdateShoppingItemRequest(isChecked = isChecked)
            when (repository.updateShoppingItem(listId, itemId, request)) {
                is AppResult.Success -> loadDetail(listId)
                is AppResult.Error -> {}
            }
        }
    }

    fun deleteItem(listId: String, itemId: Int) {
        viewModelScope.launch {
            when (repository.deleteShoppingItem(listId, itemId)) {
                is AppResult.Success -> loadDetail(listId)
                is AppResult.Error -> {}
            }
        }
    }

    fun updateItemName(listId: String, itemId: Int, name: String, amount: String?) {
        viewModelScope.launch {
            val request = UpdateShoppingItemRequest(name = name, amount = amount)
            when (repository.updateShoppingItem(listId, itemId, request)) {
                is AppResult.Success -> loadDetail(listId)
                is AppResult.Error -> {}
            }
        }
    }

    fun parseText(text: String) {
        viewModelScope.launch {
            _aiState.value = AiParseState.Loading
            when (val result = repository.parseShoppingText(text)) {
                is AppResult.Success -> _aiState.value = AiParseState.Success(result.data.items)
                is AppResult.Error -> _aiState.value = AiParseState.Error(result.message)
            }
        }
    }

    fun batchAdd(listId: String, items: List<ParsedShoppingItem>) {
        viewModelScope.launch {
            val requests = items.map { CreateShoppingItemRequest(name = it.name, amount = it.amount) }
            when (repository.batchAddItems(listId, requests)) {
                is AppResult.Success -> {
                    _aiState.value = AiParseState.Idle
                    loadDetail(listId)
                }
                is AppResult.Error -> {}
            }
        }
    }

    fun dismissAiResult() {
        _aiState.value = AiParseState.Idle
    }

    fun updateParsedItem(index: Int, name: String, amount: String) {
        val current = (_aiState.value as? AiParseState.Success) ?: return
        val updated = current.items.toMutableList()
        updated[index] = ParsedShoppingItem(name, amount)
        _aiState.value = AiParseState.Success(updated)
    }

    fun removeParsedItem(index: Int) {
        val current = (_aiState.value as? AiParseState.Success) ?: return
        val updated = current.items.toMutableList()
        updated.removeAt(index)
        if (updated.isEmpty()) {
            _aiState.value = AiParseState.Idle
        } else {
            _aiState.value = AiParseState.Success(updated)
        }
    }
}
