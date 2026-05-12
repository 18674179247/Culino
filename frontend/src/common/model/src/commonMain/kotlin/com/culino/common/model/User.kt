package com.culino.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val nickname: String? = null,
    val avatar: String? = null,
    @SerialName("role_code") val roleCode: String = "user"
)

data class User(
    val id: String,
    val username: String,
    val nickname: String?,
    val avatar: String?,
    val roleCode: String
) {
    val isAdmin: Boolean get() = roleCode == "admin"
}

fun UserDto.toDomain(): User = User(
    id = id,
    username = username,
    nickname = nickname,
    avatar = avatar,
    roleCode = roleCode
)

@Serializable
data class UserStatsDto(
    @SerialName("recipe_count") val recipeCount: Long,
    @SerialName("favorite_count") val favoriteCount: Long,
    @SerialName("cooking_log_count") val cookingLogCount: Long,
)
