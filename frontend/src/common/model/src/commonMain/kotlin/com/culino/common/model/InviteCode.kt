package com.culino.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 邀请码（对应后端 features/user/src/model.rs 的 InviteCode）。
 */
@Serializable
data class InviteCode(
    val code: String,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("max_uses") val maxUses: Int,
    @SerialName("used_count") val usedCount: Int,
    @SerialName("expires_at") val expiresAt: String? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String
) {
    val remaining: Int get() = (maxUses - usedCount).coerceAtLeast(0)
    val isExhausted: Boolean get() = usedCount >= maxUses
}

/**
 * 创建邀请码请求。
 * expiresAt 传 ISO8601 字符串，为空表示永不过期。
 */
@Serializable
data class CreateInviteCodeRequest(
    @SerialName("max_uses") val maxUses: Int? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val note: String? = null
)
