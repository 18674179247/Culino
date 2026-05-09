package com.culino.framework.storage

import com.culino.framework.network.AuthExpiredBus
import com.culino.framework.network.TokenProvider
import kotlinx.coroutines.flow.Flow

class TokenStorage(
    private val store: KeyValueStore
) : TokenProvider {

    private val tokenKey = "auth_token"

    private var cachedToken: String? = null

    override val authExpiredEvent: Flow<Unit> = AuthExpiredBus.events

    override suspend fun getToken(): String? {
        if (cachedToken != null) return cachedToken
        cachedToken = store.getString(tokenKey)
        return cachedToken
    }

    override suspend fun saveToken(token: String) {
        cachedToken = token
        store.putString(tokenKey, token)
    }

    override suspend fun clearToken() {
        cachedToken = null
        store.remove(tokenKey)
    }

    override suspend fun notifyAuthExpired() {
        clearToken()
        AuthExpiredBus.emit()
    }
}
