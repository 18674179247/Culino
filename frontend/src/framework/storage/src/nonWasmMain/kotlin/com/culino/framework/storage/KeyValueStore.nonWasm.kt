package com.culino.framework.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

private const val DATA_STORE_FILE_NAME = "culino_prefs.preferences_pb"

private class DataStoreKeyValueStore(
    private val dataStore: DataStore<Preferences>
) : KeyValueStore {
    override suspend fun getString(key: String): String? =
        dataStore.data.map { it[stringPreferencesKey(key)] }.first()

    override fun observeString(key: String): Flow<String?> =
        dataStore.data.map { it[stringPreferencesKey(key)] }

    override suspend fun putString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }
}

actual fun createKeyValueStore(parentPath: String): KeyValueStore {
    val dataStore = PreferenceDataStoreFactory.createWithPath {
        "$parentPath/$DATA_STORE_FILE_NAME".toPath()
    }
    return DataStoreKeyValueStore(dataStore)
}
