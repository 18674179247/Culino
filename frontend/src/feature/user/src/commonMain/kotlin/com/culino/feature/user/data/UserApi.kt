package com.culino.feature.user.data

import com.culino.common.util.AppResult
import com.culino.common.model.*
import com.culino.framework.network.ApiClient
import io.ktor.client.request.*
import io.ktor.http.*

class UserApi(private val apiClient: ApiClient) {

    suspend fun login(request: LoginRequest): AppResult<ApiResponse<AuthResponse>> =
        apiClient.safeRequest {
            url { path("user/login") }
            method = HttpMethod.Post
            setBody(request)
        }

    suspend fun register(request: RegisterRequest): AppResult<ApiResponse<AuthResponse>> =
        apiClient.safeRequest {
            url { path("user/register") }
            method = HttpMethod.Post
            setBody(request)
        }

    suspend fun getProfile(): AppResult<ApiResponse<UserDto>> =
        apiClient.safeRequest {
            url { path("user/me") }
            method = HttpMethod.Get
        }

    suspend fun updateProfile(request: UpdateProfileRequest): AppResult<ApiResponse<UserDto>> =
        apiClient.safeRequest {
            url { path("user/me") }
            method = HttpMethod.Put
            setBody(request)
        }

    suspend fun logout(): AppResult<ApiResponse<Unit>> =
        apiClient.safeRequest {
            url { path("user/logout") }
            method = HttpMethod.Post
        }

    // -----------------------------
    // 邀请码管理（仅 admin 可用，后端会校验 role_code）
    // -----------------------------

    suspend fun listInviteCodes(): AppResult<ApiResponse<List<InviteCode>>> =
        apiClient.safeRequest {
            url { path("user/invite-codes") }
            method = HttpMethod.Get
        }

    suspend fun createInviteCode(request: CreateInviteCodeRequest): AppResult<ApiResponse<InviteCode>> =
        apiClient.safeRequest {
            url { path("user/invite-codes") }
            method = HttpMethod.Post
            setBody(request)
        }

    suspend fun revokeInviteCode(code: String): AppResult<ApiResponse<Boolean>> =
        apiClient.safeRequest {
            url { path("user/invite-codes/$code") }
            method = HttpMethod.Delete
        }
}
