package com.culino.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.culino.core.network.AuthExpiredBus
import com.culino.core.network.TokenProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TokenStorage(
    private val dataStore: DataStore<Preferences>
) : TokenProvider {

    private val tokenKey = stringPreferencesKey("auth_token")

    private var cachedToken: String? = null

    override val authExpiredEvent: Flow<Unit> = AuthExpiredBus.events

    override suspend fun getToken(): String? {
        if (cachedToken != null) return cachedToken
        cachedToken = dataStore.data.map { prefs -> prefs[tokenKey] }.first()
        return cachedToken
    }

    override suspend fun saveToken(token: String) {
        cachedToken = token
        dataStore.edit { prefs -> prefs[tokenKey] = token }
    }

    override suspend fun clearToken() {
        cachedToken = null
        dataStore.edit { prefs -> prefs.remove(tokenKey) }
    }

    override suspend fun notifyAuthExpired() {
        clearToken()
        AuthExpiredBus.emit()
    }
}
