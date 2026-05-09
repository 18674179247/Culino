package com.culino.feature.user.domain

import com.culino.common.util.AppResult
import com.culino.common.model.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LoginUseCaseTest {

    private val fakeUser = User("u1", "test", "Test", null, "user")

    private val successRepo = object : UserRepository {
        override suspend fun login(username: String, password: String) = AppResult.Success(fakeUser)
        override suspend fun register(username: String, password: String, nickname: String?) = AppResult.Success(fakeUser)
        override suspend fun getProfile() = AppResult.Success(fakeUser)
        override suspend fun updateProfile(nickname: String?, avatar: String?) = AppResult.Success(fakeUser)
        override suspend fun logout() = AppResult.Success(Unit)
    }

    @Test
    fun loginWithEmptyUsernameReturnsError() = runTest {
        val useCase = LoginUseCase(successRepo)
        val result = useCase("", "password")
        assertIs<AppResult.Error>(result)
        assertEquals("用户名不能为空", (result as AppResult.Error).message)
    }

    @Test
    fun loginWithEmptyPasswordReturnsError() = runTest {
        val useCase = LoginUseCase(successRepo)
        val result = useCase("user", "")
        assertIs<AppResult.Error>(result)
        assertEquals("密码不能为空", (result as AppResult.Error).message)
    }

    @Test
    fun loginWithValidCredentialsDelegatesToRepo() = runTest {
        val useCase = LoginUseCase(successRepo)
        val result = useCase("test", "password")
        assertIs<AppResult.Success<User>>(result)
        assertEquals("test", result.data.username)
    }
}
