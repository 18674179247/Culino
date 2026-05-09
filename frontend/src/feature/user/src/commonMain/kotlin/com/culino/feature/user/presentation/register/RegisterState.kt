package com.culino.feature.user.presentation.register

import com.culino.common.model.User

data class RegisterState(
    val username: String = "",
    val password: String = "",
    val nickname: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val registeredUser: User? = null
)

sealed interface RegisterIntent {
    data class UpdateUsername(val username: String) : RegisterIntent
    data class UpdatePassword(val password: String) : RegisterIntent
    data class UpdateNickname(val nickname: String) : RegisterIntent
    data object Submit : RegisterIntent
    data object ClearError : RegisterIntent
}
