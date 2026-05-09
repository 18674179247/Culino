package com.culino.feature.user.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.culino.common.util.AppResult
import com.culino.feature.user.domain.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateUsername -> _state.update { it.copy(username = intent.username) }
            is LoginIntent.UpdatePassword -> _state.update { it.copy(password = intent.password) }
            is LoginIntent.Submit -> submit()
            is LoginIntent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun submit() {
        val current = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = loginUseCase(current.username, current.password)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, loggedInUser = result.data)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}
