package com.menu.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenStorageTest {

    private fun testDataStore(name: String) = PreferenceDataStoreFactory.createWithPath {
        "${System.getProperty("java.io.tmpdir")}/test_$name.preferences_pb".toPath()
    }

    @Test
    fun saveAndRetrieveToken() = runTest {
        val storage = TokenStorage(testDataStore("save_${System.nanoTime()}"))
        storage.saveToken("test-token-123")
        val token = storage.getToken()
        assertEquals("test-token-123", token)
    }

    @Test
    fun clearTokenRemovesIt() = runTest {
        val storage = TokenStorage(testDataStore("clear_${System.nanoTime()}"))
        storage.saveToken("test-token-123")
        storage.clearToken()
        val token = storage.getToken()
        assertNull(token)
    }
}
