package com.menu.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.menu.core.network.TokenProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TokenStorage(
    private val dataStore: DataStore<Preferences>
) : TokenProvider {

    private val tokenKey = stringPreferencesKey("auth_token")

    override suspend fun getToken(): String? {
        return dataStore.data.map { prefs -> prefs[tokenKey] }.first()
    }

    override suspend fun saveToken(token: String) {
        dataStore.edit { prefs -> prefs[tokenKey] = token }
    }

    override suspend fun clearToken() {
        dataStore.edit { prefs -> prefs.remove(tokenKey) }
    }
}
