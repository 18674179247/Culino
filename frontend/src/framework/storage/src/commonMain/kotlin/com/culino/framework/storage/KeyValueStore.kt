package com.culino.framework.storage

import kotlinx.coroutines.flow.Flow

interface KeyValueStore {
    suspend fun getString(key: String): String?
    fun observeString(key: String): Flow<String?>
    suspend fun putString(key: String, value: String)
    suspend fun remove(key: String)
}

expect fun createKeyValueStore(parentPath: String): KeyValueStore
