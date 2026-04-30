package com.menu.core.network

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface TokenProvider {
    suspend fun getToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}

@OptIn(ExperimentalEncodingApi::class)
fun parseUserIdFromToken(token: String): String? {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return null
        val payload = parts[1]
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val decoded = Base64.decode(padded)
        val json = decoded.decodeToString()
        // 简单解析 "sub":"xxx"
        val subIndex = json.indexOf("\"sub\"")
        if (subIndex == -1) return null
        val colonIndex = json.indexOf(":", subIndex)
        val quoteStart = json.indexOf("\"", colonIndex + 1)
        val quoteEnd = json.indexOf("\"", quoteStart + 1)
        json.substring(quoteStart + 1, quoteEnd)
    } catch (_: Exception) {
        null
    }
}
