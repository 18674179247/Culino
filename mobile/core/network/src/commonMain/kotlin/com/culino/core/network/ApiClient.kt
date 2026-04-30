package com.culino.core.network

import com.culino.core.common.AppResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

class ApiClient(@PublishedApi internal val httpClient: HttpClient) {

    suspend inline fun <reified T> safeRequest(
        block: HttpRequestBuilder.() -> Unit
    ): AppResult<T> {
        return try {
            val response: HttpResponse = httpClient.request(block)
            if (response.status.value in 200..299) {
                AppResult.Success(response.body<T>())
            } else {
                AppResult.Error("HTTP ${response.status.value}: ${response.status.description}")
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
                    val error = backendResponse.error
                    ApiResponse.Error(
                        error?.message ?: "Unknown error",
                        response.status.value
                    )
                }
            } else {
                ApiResponse.Error("HTTP ${response.status.value}: ${response.status.description}", response.status.value)
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error")
        }
    }

    suspend inline fun <reified T> get(path: String, params: Map<String, String>): ApiResponse<T> {
        return try {
            val response: HttpResponse = httpClient.get(path) {
                params.forEach { (key, value) ->
                    parameter(key, value)
                }
            }
            if (response.status.value in 200..299) {
                val backendResponse = response.body<BackendResponse<T>>()
                if (backendResponse.ok && backendResponse.data != null) {
                    ApiResponse.Success(backendResponse.data)
                } else {
                    val error = backendResponse.error
                    ApiResponse.Error(
                        error?.message ?: "Unknown error",
                        response.status.value
                    )
                }
            } else {
                ApiResponse.Error("HTTP ${response.status.value}: ${response.status.description}", response.status.value)
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error")
        }
    }

    suspend inline fun <reified T, reified R> post(path: String, body: T): ApiResponse<R> {
        return try {
            val response: HttpResponse = httpClient.post(path) {
                setBody(body)
            }
            if (response.status.value in 200..299) {
                val backendResponse = response.body<BackendResponse<R>>()
                if (backendResponse.ok && backendResponse.data != null) {
                    ApiResponse.Success(backendResponse.data)
                } else {
                    val error = backendResponse.error
                    ApiResponse.Error(
                        error?.message ?: "Unknown error",
                        response.status.value
                    )
                }
            } else {
                ApiResponse.Error("HTTP ${response.status.value}: ${response.status.description}", response.status.value)
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error")
        }
    }

    suspend inline fun <reified T, reified R> put(path: String, body: T): ApiResponse<R> {
        return try {
            val response: HttpResponse = httpClient.put(path) {
                setBody(body)
            }
            if (response.status.value in 200..299) {
                val backendResponse = response.body<BackendResponse<R>>()
                if (backendResponse.ok && backendResponse.data != null) {
                    ApiResponse.Success(backendResponse.data)
                } else {
                    val error = backendResponse.error
                    ApiResponse.Error(
                        error?.message ?: "Unknown error",
                        response.status.value
                    )
                }
            } else {
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
                    val error = backendResponse.error
                    ApiResponse.Error(
                        error?.message ?: "Unknown error",
                        response.status.value
                    )
                }
            } else {
                ApiResponse.Error("HTTP ${response.status.value}: ${response.status.description}", response.status.value)
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.message ?: "Network error")
        }
    }
}
