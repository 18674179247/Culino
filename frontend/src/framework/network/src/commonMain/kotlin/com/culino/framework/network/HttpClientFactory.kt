package com.culino.framework.network

import com.culino.common.util.Constants
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
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

        install(createClientPlugin("TokenAttach") {
            onRequest { request, _ ->
                val path = request.url.encodedPath
                if (!path.contains("/login") && !path.contains("/register")) {
                    val token = tokenProvider.getToken()
                    if (token != null) {
                        request.bearerAuth(token)
                    }
                }
            }
        })

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    io.github.aakira.napier.Napier.d(message, tag = "HTTP")
                }
            }
            level = if (debugLogging) LogLevel.INFO else LogLevel.NONE
            sanitizeHeader { it.equals(HttpHeaders.Authorization, ignoreCase = true) }
        }

        defaultRequest {
            url(Constants.API_BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }
}
