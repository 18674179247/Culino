package com.menu.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AppResultTest {

    @Test
    fun successWrapsValue() {
        val result: AppResult<String> = AppResult.Success("hello")
        assertIs<AppResult.Success<String>>(result)
        assertEquals("hello", result.data)
    }

    @Test
    fun errorWrapsMessage() {
        val result: AppResult<String> = AppResult.Error("fail")
        assertIs<AppResult.Error>(result)
        assertEquals("fail", result.message)
    }

    @Test
    fun errorExceptionIsOptional() {
        val result = AppResult.Error("fail")
        assertNull(result.exception)
    }

    @Test
    fun mapTransformsSuccess() {
        val result: AppResult<Int> = AppResult.Success("hello").map { it.length }
        assertIs<AppResult.Success<Int>>(result)
        assertEquals(5, result.data)
    }

    @Test
    fun mapPassesThroughError() {
        val result: AppResult<Int> = AppResult.Error("fail").map { 42 }
        assertIs<AppResult.Error>(result)
        assertEquals("fail", result.message)
    }
}
