package com.culino.framework.storage

import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.w3c.dom.get

private class LocalStorageKeyValueStore : KeyValueStore {
    private val tick = MutableStateFlow(0)

    override suspend fun getString(key: String): String? = localStorage[key]

    override fun observeString(key: String): Flow<String?> = tick.map { localStorage[key] }

    override suspend fun putString(key: String, value: String) {
        localStorage.setItem(key, value)
        tick.value += 1
    }

    override suspend fun remove(key: String) {
        localStorage.removeItem(key)
        tick.value += 1
    }
}

actual fun createKeyValueStore(parentPath: String): KeyValueStore = LocalStorageKeyValueStore()
