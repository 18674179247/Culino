package com.culino.core.network

import com.culino.core.common.Constants
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createHttpClient(tokenProvider: TokenProvider): HttpClient {
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

                refreshTokens {
                    val token = tokenProvider.getToken()
                    token?.let { BearerTokens(it, "") }
                }

                sendWithoutRequest { request ->
                    !request.url.encodedPath.contains("/login") &&
                    !request.url.encodedPath.contains("/register")
                }
            }
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    io.github.aakira.napier.Napier.d(message, tag = "HTTP")
                }
            }
            level = LogLevel.HEADERS
        }

        defaultRequest {
            url(Constants.API_BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }
}
