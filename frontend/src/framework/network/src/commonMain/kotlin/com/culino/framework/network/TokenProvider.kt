package com.culino.framework.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface TokenProvider {
    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
    val authExpiredEvent: Flow<Unit>
    suspend fun notifyAuthExpired()
}

object AuthExpiredBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: Flow<Unit> = _events.asSharedFlow()

    fun emit() {
        _events.tryEmit(Unit)
    }
}

@OptIn(ExperimentalEncodingApi::class)
fun parseUserIdFromToken(token: String): String? {
    return parseTokenField(token, "sub")
}

@OptIn(ExperimentalEncodingApi::class)
fun parseRoleFromToken(token: String): String? {
    return parseTokenField(token, "role_code")
}

@OptIn(ExperimentalEncodingApi::class)
private fun parseTokenField(token: String, field: String): String? {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return null
        val payload = parts[1]
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = Base64.decode(padded)
        val json = decoded.decodeToString()
        val fieldIndex = json.indexOf("\"$field\"")
        if (fieldIndex == -1) return null
        val colonIndex = json.indexOf(":", fieldIndex)
        val quoteStart = json.indexOf("\"", colonIndex + 1)
        val quoteEnd = json.indexOf("\"", quoteStart + 1)
        json.substring(quoteStart + 1, quoteEnd)
    } catch (_: Exception) {
        null
    }
}
