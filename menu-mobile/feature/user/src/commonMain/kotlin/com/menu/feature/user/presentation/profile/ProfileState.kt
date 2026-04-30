package com.menu.feature.user.presentation.profile

import com.menu.core.model.User

data class ProfileStats(
    val recipeCount: Int = 0,
    val favoriteCount: Int = 0,
    val cookingLogCount: Int = 0
)

data class ProfileState(
    val user: User? = null,
    val stats: ProfileStats = ProfileStats(),
    val editNickname: String = "",
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val error: String? = null,
    val loggedOut: Boolean = false
)

sealed interface ProfileIntent {
    data object LoadProfile : ProfileIntent
    data object ToggleEdit : ProfileIntent
    data class UpdateNickname(val nickname: String) : ProfileIntent
    data object SaveProfile : ProfileIntent
    data class UploadAvatar(val bytes: ByteArray, val fileName: String, val contentType: String) : ProfileIntent
    data object Logout : ProfileIntent
    data object ClearError : ProfileIntent
}
