package com.culino.feature.user.domain

import com.culino.common.model.InviteCode
import com.culino.common.util.AppResult

/**
 * 邀请码管理 UseCase 集合，供 admin 界面使用。
 * 后端会校验 role_code == "admin"，非管理员调用会返回 403。
 */
class InviteCodeUseCases(private val repository: UserRepository) {
    suspend fun list(): AppResult<List<InviteCode>> = repository.listInviteCodes()

    suspend fun create(maxUses: Int?, expiresAt: String?, note: String?): AppResult<InviteCode> =
        repository.createInviteCode(maxUses, expiresAt, note)

    suspend fun revoke(code: String): AppResult<Unit> = repository.revokeInviteCode(code)
}
