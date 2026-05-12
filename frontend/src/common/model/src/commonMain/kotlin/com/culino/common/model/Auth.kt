package com.culino.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String? = null,
    @SerialName("invite_code") val inviteCode: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto
)

@Serializable
data class UpdateProfileRequest(
    val nickname: String? = null,
    val avatar: String? = null
)
