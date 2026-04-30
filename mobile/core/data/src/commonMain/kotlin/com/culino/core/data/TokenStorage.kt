package com.culino.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.culino.core.network.TokenProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TokenStorage(
    private val dataStore: DataStore<Preferences>
) : TokenProvider {

    private val tokenKey = stringPreferencesKey("auth_token")

    // 内存缓存，避免 DataStore 异步读取延迟导致 Ktor Auth 插件读不到 token
    private var cachedToken: String? = null

    override suspend fun getToken(): String? {
        if (cachedToken != null) return cachedToken
        cachedToken = dataStore.data.map { prefs -> prefs[tokenKey] }.first()
        return cachedToken
    }

    override suspend fun saveToken(token: String) {
        cachedToken = token // 先写内存，Auth 插件下一次 loadTokens 立即可读
        dataStore.edit { prefs -> prefs[tokenKey] = token }
    }

    override suspend fun clearToken() {
        cachedToken = null
        dataStore.edit { prefs -> prefs.remove(tokenKey) }
    }
}
