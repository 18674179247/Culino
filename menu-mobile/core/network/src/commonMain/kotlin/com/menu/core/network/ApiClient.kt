package com.menu.core.network

import com.menu.core.common.AppResult
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
}
