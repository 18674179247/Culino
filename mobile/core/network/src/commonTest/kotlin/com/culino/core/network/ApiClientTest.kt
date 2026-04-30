package com.culino.core.network

import com.culino.core.common.AppResult
import com.culino.core.model.ApiResponse
import com.culino.core.model.UserDto
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApiClientTest {

    private fun mockClient(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        return HttpClient(MockEngine { _ ->
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Test
    fun safeApiCallReturnsSuccessOnOkResponse() = runTest {
        val client = mockClient(
            """{"ok":true,"data":{"id":"1","username":"test","nickname":"Test","avatar":null,"role_code":"user"},"error":null}"""
        )
        val apiClient = ApiClient(client)
        val result = apiClient.safeRequest<ApiResponse<UserDto>> {
            url { path("user/me") }
            method = HttpMethod.Get
        }
        assertIs<AppResult.Success<ApiResponse<UserDto>>>(result)
        assertEquals("test", result.data.data?.username)
    }

    @Test
    fun safeApiCallReturnsErrorOnServerError() = runTest {
        val client = mockClient(
            """{"ok":false,"data":null,"error":"Not found"}""",
            HttpStatusCode.NotFound
        )
        val apiClient = ApiClient(client)
        val result = apiClient.safeRequest<ApiResponse<UserDto>> {
            url { path("user/me") }
            method = HttpMethod.Get
        }
        assertIs<AppResult.Error>(result)
    }
}
