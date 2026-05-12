package com.culino.feature.user.domain

import com.culino.common.util.AppResult
import com.culino.common.model.InviteCode
import com.culino.common.model.User

interface UserRepository {
    suspend fun login(username: String, password: String): AppResult<User>
    suspend fun register(username: String, password: String, nickname: String?, inviteCode: String): AppResult<User>
    suspend fun getProfile(): AppResult<User>
    suspend fun updateProfile(nickname: String?, avatar: String?): AppResult<User>
    suspend fun logout(): AppResult<Unit>

    // 邀请码管理（仅 admin）
    suspend fun listInviteCodes(): AppResult<List<InviteCode>>
    suspend fun createInviteCode(maxUses: Int?, expiresAt: String?, note: String?): AppResult<InviteCode>
    suspend fun revokeInviteCode(code: String): AppResult<Unit>
}
