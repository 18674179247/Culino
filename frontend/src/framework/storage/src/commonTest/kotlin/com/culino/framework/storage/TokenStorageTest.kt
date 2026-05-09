package com.culino.framework.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class InMemoryKeyValueStore : KeyValueStore {
    private val state = MutableStateFlow<Map<String, String>>(emptyMap())
    override suspend fun getString(key: String): String? = state.value[key]
    override fun observeString(key: String): Flow<String?> = state.map { it[key] }
    override suspend fun putString(key: String, value: String) {
        state.value = state.value + (key to value)
    }
    override suspend fun remove(key: String) {
        state.value = state.value - key
    }
}

class TokenStorageTest {

    @Test
    fun saveAndRetrieveToken() = runTest {
        val storage = TokenStorage(InMemoryKeyValueStore())
        storage.saveToken("test-token-123")
        val token = storage.getToken()
        assertEquals("test-token-123", token)
    }

    @Test
    fun clearTokenRemovesIt() = runTest {
        val storage = TokenStorage(InMemoryKeyValueStore())
        storage.saveToken("test-token-123")
        storage.clearToken()
        val token = storage.getToken()
        assertNull(token)
    }
}
