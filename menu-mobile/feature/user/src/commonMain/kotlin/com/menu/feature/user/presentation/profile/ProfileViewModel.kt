package com.menu.feature.user.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.AppResult
import com.menu.feature.user.domain.GetProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    // 移除 init 块中的自动加载，改为在 UI 首次显示时手动触发
    // init { onIntent(ProfileIntent.LoadProfile) }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LoadProfile -> loadProfile()
            is ProfileIntent.ToggleEdit -> {
                val current = _state.value
                _state.update {
                    it.copy(
                        isEditing = !current.isEditing,
                        editNickname = current.user?.nickname ?: ""
                    )
                }
            }
            is ProfileIntent.UpdateNickname -> _state.update { it.copy(editNickname = intent.nickname) }
            is ProfileIntent.SaveProfile -> saveProfile()
            is ProfileIntent.Logout -> logout()
            is ProfileIntent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadProfile() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = getProfileUseCase.getProfile()) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, user = result.data, editNickname = result.data.nickname ?: "")
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    private fun saveProfile() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val nickname = _state.value.editNickname.ifBlank { null }
            when (val result = getProfileUseCase.updateProfile(nickname, null)) {
                is AppResult.Success -> _state.update {
                    it.copy(isLoading = false, user = result.data, isEditing = false)
                }
                is AppResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            getProfileUseCase.logout()
            _state.update { it.copy(loggedOut = true) }
        }
    }
}
