package com.culino.feature.user.data

import com.culino.common.util.AppResult
import com.culino.common.model.*
import com.culino.framework.network.ApiClient
import com.culino.framework.network.TokenProvider
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UserRepositoryImplTest {

    private val fakeTokenProvider = object : TokenProvider {
        var token: String? = null
        private val expiredBus = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        override suspend fun getToken(): String? = token
        override suspend fun saveToken(t: String) { token = t }
        override suspend fun clearToken() { token = null }
        override val authExpiredEvent: Flow<Unit> = expiredBus.asSharedFlow()
        override suspend fun notifyAuthExpired() { expiredBus.tryEmit(Unit) }
    }

    private fun mockApiClient(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK): ApiClient {
        val client = HttpClient(MockEngine { _ ->
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
        return ApiClient(client)
    }

    @Test
    fun loginSuccessSavesTokenAndReturnsUser() = runTest {
        val apiClient = mockApiClient(
            """{"ok":true,"data":{"token":"jwt-123","user":{"id":"u1","username":"test","nickname":"Test","avatar":null,"role_code":"user"}},"error":null}"""
        )
        val repo = UserRepositoryImpl(UserApi(apiClient), fakeTokenProvider)
        val result = repo.login("test", "pass")

        assertIs<AppResult.Success<*>>(result)
        val user = (result as AppResult.Success).data
        assertEquals("test", user.username)
        assertEquals("jwt-123", fakeTokenProvider.token)
    }

    @Test
    fun loginFailureReturnsError() = runTest {
        val apiClient = mockApiClient(
            """{"ok":false,"data":null,"error":"Invalid credentials"}"""
        )
        val repo = UserRepositoryImpl(UserApi(apiClient), fakeTokenProvider)
        val result = repo.login("test", "wrong")

        assertIs<AppResult.Error>(result)
        assertEquals("Invalid credentials", (result as AppResult.Error).message)
    }

    @Test
    fun logoutClearsToken() = runTest {
        fakeTokenProvider.token = "jwt-123"
        val apiClient = mockApiClient("""{"ok":true,"data":null,"error":null}""")
        val repo = UserRepositoryImpl(UserApi(apiClient), fakeTokenProvider)
        repo.logout()

        assertEquals(null, fakeTokenProvider.token)
    }
}
