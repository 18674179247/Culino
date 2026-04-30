package com.culino.feature.user.domain

import com.culino.core.common.AppResult
import com.culino.core.model.User

class GetProfileUseCase(private val repository: UserRepository) {
    suspend fun getProfile(): AppResult<User> = repository.getProfile()
    suspend fun updateProfile(nickname: String?, avatar: String?): AppResult<User> =
        repository.updateProfile(nickname, avatar)
    suspend fun logout(): AppResult<Unit> = repository.logout()
}
