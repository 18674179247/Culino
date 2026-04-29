package com.menu.feature.user.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.menu.core.common.AppResult
import com.menu.core.network.ImageUploadApi
import com.menu.feature.user.domain.GetProfileUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val imageUploadApi: ImageUploadApi
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

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
            is ProfileIntent.UploadAvatar -> uploadAvatar(intent.bytes, intent.fileName, intent.contentType)
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

    private fun uploadAvatar(bytes: ByteArray, fileName: String, contentType: String) {
        _state.update { it.copy(isUploadingAvatar = true) }
        viewModelScope.launch {
            when (val uploadResult = imageUploadApi.uploadImage(bytes, fileName, contentType)) {
                is AppResult.Success -> {
                    val avatarUrl = uploadResult.data
                    Napier.d("Upload success, avatarUrl=$avatarUrl", tag = "Profile")
                    when (val updateResult = getProfileUseCase.updateProfile(
                        _state.value.user?.nickname,
                        avatarUrl
                    )) {
                        is AppResult.Success -> {
                            Napier.d("Profile updated, avatar=${updateResult.data.avatar}", tag = "Profile")
                            _state.update {
                                it.copy(isUploadingAvatar = false, user = updateResult.data)
                            }
                        }
                        is AppResult.Error -> _state.update {
                            it.copy(isUploadingAvatar = false, error = updateResult.message)
                        }
                    }
                }
                is AppResult.Error -> _state.update {
                    it.copy(isUploadingAvatar = false, error = uploadResult.message)
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
