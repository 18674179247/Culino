package com.culino.framework.network

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

actual fun createHttpEngine(): HttpClientEngineFactory<*> = Darwin
