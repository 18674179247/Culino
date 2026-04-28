package com.menu.feature.user.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.AppResult
import com.menu.feature.user.domain.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UpdateUsername -> _state.update { it.copy(username = intent.username) }
            is RegisterIntent.UpdatePassword -> _state.update { it.copy(password = intent.password) }
            is RegisterIntent.UpdateNickname -> _state.update { it.copy(nickname = intent.nickname) }
            is RegisterIntent.Submit -> submit()
            is RegisterIntent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun submit() {
        val current = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val nickname = current.nickname.ifBlank { null }
            when (val result = registerUseCase(current.username, current.password, nickname)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, registeredUser = result.data)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}
