package com.menu.feature.social.presentation.cookinglog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.AppResult
import com.menu.feature.social.data.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CookingLogViewModel(
    private val repository: SocialRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CookingLogState>(CookingLogState.Loading)
    val state: StateFlow<CookingLogState> = _state.asStateFlow()

    init {
        loadCookingLogs()
    }

    fun loadCookingLogs() {
        viewModelScope.launch {
            _state.value = CookingLogState.Loading
            when (val result = repository.getCookingLogs()) {
                is AppResult.Success -> {
                    _state.value = CookingLogState.Success(result.data)
                }
                is AppResult.Error -> {
                    _state.value = CookingLogState.Error(result.message)
                }
            }
        }
    }

    fun deleteCookingLog(id: String) {
        viewModelScope.launch {
            when (repository.deleteCookingLog(id)) {
                is AppResult.Success -> loadCookingLogs()
                is AppResult.Error -> {
                    // Could show error toast
                }
            }
        }
    }
}
