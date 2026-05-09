package com.culino.framework.storage

import kotlinx.coroutines.flow.Flow

class PreferencesStorage(
    private val store: KeyValueStore
) {
    fun getString(key: String): Flow<String?> = store.observeString(key)

    suspend fun putString(key: String, value: String) = store.putString(key, value)

    suspend fun remove(key: String) = store.remove(key)
}
