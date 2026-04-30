package com.menu.feature.user.domain

import com.menu.core.common.AppResult
import com.menu.core.model.User

class LoginUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(username: String, password: String): AppResult<User> {
        if (username.isBlank()) return AppResult.Error("用户名不能为空")
        if (password.isBlank()) return AppResult.Error("密码不能为空")
        return repository.login(username, password)
    }
}
