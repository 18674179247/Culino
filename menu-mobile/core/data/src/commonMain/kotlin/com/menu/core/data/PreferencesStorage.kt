package com.menu.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesStorage(
    private val dataStore: DataStore<Preferences>
) {
    fun getString(key: String): Flow<String?> {
        return dataStore.data.map { prefs -> prefs[stringPreferencesKey(key)] }
    }

    suspend fun putString(key: String, value: String) {
        dataStore.edit { prefs -> prefs[stringPreferencesKey(key)] = value }
    }

    suspend fun remove(key: String) {
        dataStore.edit { prefs -> prefs.remove(stringPreferencesKey(key)) }
    }
}
