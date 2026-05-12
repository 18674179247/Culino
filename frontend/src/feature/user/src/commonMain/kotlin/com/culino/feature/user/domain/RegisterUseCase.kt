package com.culino.feature.user.domain

import com.culino.common.util.AppResult
import com.culino.common.model.User

class RegisterUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(
        username: String,
        password: String,
        nickname: String?,
        inviteCode: String
    ): AppResult<User> {
        if (username.isBlank()) return AppResult.Error("用户名不能为空")
        if (username.length < 2 || username.length > 50) return AppResult.Error("用户名长度 2-50 个字符")
        validatePassword(password)?.let { return AppResult.Error(it) }
        if (inviteCode.isBlank()) return AppResult.Error("邀请码不能为空")
        return repository.register(username, password, nickname, inviteCode.trim())
    }

    /**
     * 密码强度校验（与后端 features/user/src/model.rs::validate_password_strength 保持一致）：
     * 8-18 位，必须同时包含字母和数字。
     */
    fun validatePassword(password: String): String? {
        if (password.length !in 8..18) return "密码长度必须为 8-18 位"
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasLetter || !hasDigit) return "密码必须同时包含字母和数字"
        return null
    }
}
