package com.culino.common.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

fun <T> Flow<T>.asResult(): Flow<AppResult<T>> =
    map<T, AppResult<T>> { AppResult.Success(it) }
        .catch { emit(AppResult.Error(it.message ?: "Unknown error", it)) }
