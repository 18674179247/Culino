package com.culino.feature.user.data

import com.culino.common.util.AppResult
import com.culino.common.model.*
import com.culino.framework.network.TokenProvider
import com.culino.feature.user.domain.UserRepository

class UserRepositoryImpl(
    private val userApi: UserApi,
    private val tokenProvider: TokenProvider
) : UserRepository {

    override suspend fun login(username: String, password: String): AppResult<User> {
        return when (val result = userApi.login(LoginRequest(username, password))) {
            is AppResult.Success -> {
                val response = result.data
                val authData = response.data
                if (response.ok && authData != null) {
                    tokenProvider.saveToken(authData.token)
                    AppResult.Success(authData.user.toDomain())
                } else {
                    AppResult.Error(response.error ?: "Login failed")
                }
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun register(username: String, password: String, nickname: String?): AppResult<User> {
        return when (val result = userApi.register(RegisterRequest(username, password, nickname))) {
            is AppResult.Success -> {
                val response = result.data
                val authData = response.data
                if (response.ok && authData != null) {
                    tokenProvider.saveToken(authData.token)
                    AppResult.Success(authData.user.toDomain())
                } else {
                    AppResult.Error(response.error ?: "Registration failed")
                }
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun getProfile(): AppResult<User> {
        return when (val result = userApi.getProfile()) {
            is AppResult.Success -> {
                val response = result.data
                val userData = response.data
                if (response.ok && userData != null) {
                    AppResult.Success(userData.toDomain())
                } else {
                    AppResult.Error(response.error ?: "Failed to get profile")
                }
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun updateProfile(nickname: String?, avatar: String?): AppResult<User> {
        return when (val result = userApi.updateProfile(UpdateProfileRequest(nickname, avatar))) {
            is AppResult.Success -> {
                val response = result.data
                val userData = response.data
                if (response.ok && userData != null) {
                    AppResult.Success(userData.toDomain())
                } else {
                    AppResult.Error(response.error ?: "Failed to update profile")
                }
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        userApi.logout()
        tokenProvider.clearToken()
        return AppResult.Success(Unit)
    }
}
