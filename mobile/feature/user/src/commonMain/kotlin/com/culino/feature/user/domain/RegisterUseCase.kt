package com.culino.feature.user.domain

import com.culino.core.common.AppResult
import com.culino.core.model.User

class RegisterUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(username: String, password: String, nickname: String?): AppResult<User> {
        if (username.isBlank()) return AppResult.Error("用户名不能为空")
        if (password.length < 6) return AppResult.Error("密码至少6位")
        return repository.register(username, password, nickname)
    }
}
