package com.culino.feature.user.domain

import com.culino.common.util.AppResult
import com.culino.common.model.User

class RegisterUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(username: String, password: String, nickname: String?): AppResult<User> {
        if (username.isBlank()) return AppResult.Error("用户名不能为空")
        if (password.length < 6) return AppResult.Error("密码至少6位")
        return repository.register(username, password, nickname)
    }
}
