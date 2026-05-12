package com.culino.framework.network

import com.culino.common.util.Constants
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createHttpClient(
    tokenProvider: TokenProvider,
    debugLogging: Boolean = false
): HttpClient {
    return HttpClient(createHttpEngine()) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenProvider.getToken()
                    token?.let { BearerTokens(it, "") }
                }

                // 后端目前没有 refresh 接口,采用"一次 401 即登出"策略。
                // 返回 null → Ktor 不重试,401 透传给 HttpResponseValidator,
                // 后者调 notifyAuthExpired 触发登出流程。
                refreshTokens { null }

                sendWithoutRequest { request ->
                    !request.url.encodedPath.contains("/login") &&
                    !request.url.encodedPath.contains("/register")
                }
            }
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    tokenProvider.notifyAuthExpired()
                }
            }
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    io.github.aakira.napier.Napier.d(message, tag = "HTTP")
                }
            }
            level = if (debugLogging) LogLevel.INFO else LogLevel.NONE
            // 防止 Authorization 在 HEADERS / ALL 级别下被打印
            sanitizeHeader { it.equals(HttpHeaders.Authorization, ignoreCase = true) }
        }

        defaultRequest {
            url(Constants.API_BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }
}
