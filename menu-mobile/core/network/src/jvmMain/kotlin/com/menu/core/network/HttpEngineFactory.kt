package com.menu.core.network

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*

actual fun createHttpEngine(): HttpClientEngineFactory<*> = CIO
