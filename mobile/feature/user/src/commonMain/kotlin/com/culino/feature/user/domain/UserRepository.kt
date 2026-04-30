package com.culino.feature.user.domain

import com.culino.core.common.AppResult
import com.culino.core.model.User

interface UserRepository {
    suspend fun login(username: String, password: String): AppResult<User>
    suspend fun register(username: String, password: String, nickname: String?): AppResult<User>
    suspend fun getProfile(): AppResult<User>
    suspend fun updateProfile(nickname: String?, avatar: String?): AppResult<User>
    suspend fun logout(): AppResult<Unit>
}
