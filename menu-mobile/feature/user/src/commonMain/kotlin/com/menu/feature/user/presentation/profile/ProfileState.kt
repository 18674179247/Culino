package com.menu.feature.user.presentation.profile

import com.menu.core.model.User

data class ProfileState(
    val user: User? = null,
    val editNickname: String = "",
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val error: String? = null,
    val loggedOut: Boolean = false
)

sealed interface ProfileIntent {
    data object LoadProfile : ProfileIntent
    data object ToggleEdit : ProfileIntent
    data class UpdateNickname(val nickname: String) : ProfileIntent
    data object SaveProfile : ProfileIntent
    data object Logout : ProfileIntent
    data object ClearError : ProfileIntent
}
