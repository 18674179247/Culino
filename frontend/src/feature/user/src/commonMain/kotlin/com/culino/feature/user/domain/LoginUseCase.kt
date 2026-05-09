package com.culino.feature.user.domain

import com.culino.common.util.AppResult
import com.culino.common.model.User

class LoginUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(username: String, password: String): AppResult<User> {
        if (username.isBlank()) return AppResult.Error("用户名不能为空")
        if (password.isBlank()) return AppResult.Error("密码不能为空")
        return repository.login(username, password)
    }
}
