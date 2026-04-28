package com.menu.core.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun deserializeSuccessResponse() {
        val raw = """{"ok":true,"data":{"id":"abc","username":"test","nickname":"Test User","avatar":null,"role_code":"user"},"error":null}"""
        val response = json.decodeFromString<ApiResponse<UserDto>>(raw)
        assertTrue(response.ok)
        assertEquals("abc", response.data?.id)
        assertEquals("test", response.data?.username)
        assertNull(response.error)
    }

    @Test
    fun deserializeErrorResponse() {
        val raw = """{"ok":false,"data":null,"error":"Invalid credentials"}"""
        val response = json.decodeFromString<ApiResponse<UserDto>>(raw)
        assertTrue(!response.ok)
        assertNull(response.data)
        assertEquals("Invalid credentials", response.error)
    }
}
