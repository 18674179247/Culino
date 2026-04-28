package com.menu.feature.user.presentation.login

import com.menu.core.model.User

data class LoginState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedInUser: User? = null
)

sealed interface LoginIntent {
    data class UpdateUsername(val username: String) : LoginIntent
    data class UpdatePassword(val password: String) : LoginIntent
    data object Submit : LoginIntent
    data object ClearError : LoginIntent
}
