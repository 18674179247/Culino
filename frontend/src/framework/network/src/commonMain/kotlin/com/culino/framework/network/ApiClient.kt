package com.culino.framework.network

import com.culino.common.util.AppResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ErrorResponse(
    val ok: Boolean = false,
    val error: ErrorDetail? = null
)

class ApiClient(
    @PublishedApi internal val httpClient: HttpClient,
    @PublishedApi internal val tokenProvider: TokenProvider
) {

    @PublishedApi
    internal val errorJson = Json { ignoreUnknownKeys = true }

    @PublishedApi
    internal suspend fun extractErrorMessage(response: HttpResponse): String {
        return try {
            val text = response.body<String>()
            val parsed = errorJson.decodeFromString<ErrorResponse>(text)
            parsed.error?.message ?: "HTTP ${response.status.value}"
        } catch (_: Exception) {
            "HTTP ${response.status.value}: ${response.status.description}"
        }
    }

    @PublishedApi
    internal suspend fun handleNon2xx(response: HttpResponse): AppResult<Nothing> {
        val path = response.call.request.url.encodedPath
        val isAuthEndpoint = path.contains("/login") || path.contains("/register")

        if (response.status == HttpStatusCode.Unauthorized && !isAuthEndpoint) {
            tokenProvider.notifyAuthExpired()
        }

        val msg = extractErrorMessage(response)
        return AppResult.Error(msg)
    }

    suspend inline fun <reified T> safeRequest(
        block: HttpRequestBuilder.() -> Unit
    ): AppResult<T> {
        return try {
            val response: HttpResponse = httpClient.request(block)
            if (response.status.value in 200..299) {
                AppResult.Success(response.body<T>())
            } else {
                handleNon2xx(response)
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Network error", e)
        }
    }

    suspend inline fun <reified T> get(path: String): ApiResponse<T> {
        return try {
            val response: HttpResponse = httpClient.get(path)
            if (response.status.value in 200..299) {
                val backendResponse = response.body<BackendResponse<T>>()
                if (backendResponse.ok && backendResponse.data != null) {
                    ApiResponse.Success(backendResponse.data)
                } else {
                    ApiResponse.Error(backendResponse.error?.message ?: "Unknown error", response.status.value)
                }
            } else {
                if (response.status == HttpStatusCode.Unauthorized) {
                    tokenProvider.notifyAuthExpired()
                }
                ApiResponse.Error("HTTP ${response.status.value}: ${response.status.description}", response.status.value)
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error")
        }
    }

    suspend inline fun <reified T> get(path: String, params: Map<String, String>): ApiResponse<T> {
        return try {
            val response: HttpResponse = httpClient.get(path) {
                params.forEach { (key, value) -> parameter(key, value) }
            }
            if (response.status.value in 200..299) {
                val backendResponse = response.body<BackendResponse<T>>()
                if (backendResponse.ok && backendResponse.data != null) {
                    ApiResponse.Success(backendResponse.data)
                } else {
                    ApiResponse.Error(backendResponse.error?.message ?: "Unknown error", response.status.value)
                }
            } else {
                if (response.status == HttpStatusCode.Unauthorized) {
                    tokenProvider.notifyAuthExpired()
                }
                ApiResponse.Error("HTTP ${response.status.value}: ${response.status.description}", response.status.value)
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error")
        }
    }

    suspend inline fun <reified T, reified R> post(path: String, body: T): ApiResponse<R> {
        return try {
            val response: HttpResponse = httpClient.post(path) { setBody(body) }
            if (response.status.value in 200..299) {
                val backendResponse = response.body<BackendResponse<R>>()
                if (backendResponse.ok && backendResponse.data != null) {
                    ApiResponse.Success(backendResponse.data)
                } else {
                    ApiResponse.Error(backendResponse.error?.message ?: "Unknown error", response.status.value)
                }
            } else {
                if (response.status == HttpStatusCode.Unauthorized) {
                    tokenProvider.notifyAuthExpired()
                }
                ApiResponse.Error("HTTP ${response.status.value}: ${response.status.description}", response.status.value)
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error")
        }
    }

    suspend inline fun <reified T, reified R> put(path: String, body: T): ApiResponse<R> {
        return try {
            val response: HttpResponse = httpClient.put(path) { setBody(body) }
            if (response.status.value in 200..299) {
                val backendResponse = response.body<BackendResponse<R>>()
                if (backendResponse.ok && backendResponse.data != null) {
                    ApiResponse.Success(backendResponse.data)
                } else {
                    ApiResponse.Error(backendResponse.error?.message ?: "Unknown error", response.status.value)
                }
            } else {
                if (response.status == HttpStatusCode.Unauthorized) {
                    tokenProvider.notifyAuthExpired()
                }
                ApiResponse.Error("HTTP ${response.status.value}: ${response.status.description}", response.status.value)
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error")
        }
    }

    suspend inline fun <reified T> delete(path: String): ApiResponse<T> {
        return try {
            val response: HttpResponse = httpClient.delete(path)
            if (response.status.value in 200..299) {
                val backendResponse = response.body<BackendResponse<T>>()
                if (backendResponse.ok && backendResponse.data != null) {
                    ApiResponse.Success(backendResponse.data)
                } else {
                    ApiResponse.Error(backendResponse.error?.message ?: "Unknown error", response.status.value)
                }
            } else {
                if (response.status == HttpStatusCode.Unauthorized) {
                    tokenProvider.notifyAuthExpired()
                }
                ApiResponse.Error("HTTP ${response.status.value}: ${response.status.description}", response.status.value)
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error")
        }
    }
}
