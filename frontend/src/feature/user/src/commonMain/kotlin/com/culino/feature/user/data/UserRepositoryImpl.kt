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

    override suspend fun register(
        username: String,
        password: String,
        nickname: String?,
        inviteCode: String
    ): AppResult<User> {
        return when (val result = userApi.register(RegisterRequest(username, password, nickname, inviteCode))) {
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

    override suspend fun listInviteCodes(): AppResult<List<InviteCode>> {
        return when (val result = userApi.listInviteCodes()) {
            is AppResult.Success -> {
                val response = result.data
                if (response.ok) {
                    AppResult.Success(response.data ?: emptyList())
                } else {
                    AppResult.Error(response.error ?: "加载邀请码失败")
                }
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun createInviteCode(
        maxUses: Int?,
        expiresAt: String?,
        note: String?
    ): AppResult<InviteCode> {
        val req = CreateInviteCodeRequest(maxUses, expiresAt, note)
        return when (val result = userApi.createInviteCode(req)) {
            is AppResult.Success -> {
                val response = result.data
                val data = response.data
                if (response.ok && data != null) {
                    AppResult.Success(data)
                } else {
                    AppResult.Error(response.error ?: "创建邀请码失败")
                }
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun revokeInviteCode(code: String): AppResult<Unit> {
        return when (val result = userApi.revokeInviteCode(code)) {
            is AppResult.Success -> {
                val response = result.data
                if (response.ok) {
                    AppResult.Success(Unit)
                } else {
                    AppResult.Error(response.error ?: "吊销邀请码失败")
                }
            }
            is AppResult.Error -> result
        }
    }
}
