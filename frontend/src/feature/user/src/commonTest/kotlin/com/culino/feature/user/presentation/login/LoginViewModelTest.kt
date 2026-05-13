package com.culino.feature.user.presentation.login

import app.cash.turbine.test
import com.culino.common.util.AppResult
import com.culino.common.model.InviteCode
import com.culino.common.model.User
import com.culino.feature.user.domain.LoginUseCase
import com.culino.feature.user.domain.UserRepository
import com.culino.feature.user.domain.UserStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeUser = User("u1", "test", "Test", null, "user")

    private fun fakeRepo(result: AppResult<User> = AppResult.Success(fakeUser)) = object : UserRepository {
        override suspend fun login(username: String, password: String) = result
        override suspend fun register(username: String, password: String, nickname: String?, inviteCode: String) = result
        override suspend fun getProfile() = result
        override suspend fun getMyStats() = AppResult.Success(UserStats(0, 0, 0))
        override suspend fun updateProfile(nickname: String?, avatar: String?) = result
        override suspend fun logout() = AppResult.Success(Unit)
        override suspend fun listInviteCodes() = AppResult.Success(emptyList<InviteCode>())
        override suspend fun createInviteCode(maxUses: Int?, expiresAt: String?, note: String?) = AppResult.Error("not implemented")
        override suspend fun revokeInviteCode(code: String) = AppResult.Success(Unit)
    }

    @BeforeTest
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun updateUsernameUpdatesState() = runTest {
        val vm = LoginViewModel(LoginUseCase(fakeRepo()))
        vm.state.test {
            assertEquals("", awaitItem().username)
            vm.onIntent(LoginIntent.UpdateUsername("test"))
            assertEquals("test", awaitItem().username)
        }
    }

    @Test
    fun submitWithValidCredentialsSetsLoggedInUser() = runTest {
        val vm = LoginViewModel(LoginUseCase(fakeRepo()))
        vm.onIntent(LoginIntent.UpdateUsername("test"))
        vm.onIntent(LoginIntent.UpdatePassword("password"))
        vm.state.test {
            val initial = awaitItem()
            vm.onIntent(LoginIntent.Submit)
            val loading = awaitItem()
            assertEquals(true, loading.isLoading)
            val done = awaitItem()
            assertEquals(false, done.isLoading)
            assertEquals("test", done.loggedInUser?.username)
            assertNull(done.error)
        }
    }

    @Test
    fun submitWithEmptyFieldsSetsError() = runTest {
        val vm = LoginViewModel(LoginUseCase(fakeRepo()))
        vm.state.test {
            awaitItem() // initial
            vm.onIntent(LoginIntent.Submit)
            awaitItem() // loading state
            val result = awaitItem() // final state with error
            assertEquals("用户名不能为空", result.error)
        }
    }
}
