package com.culino.framework.network

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*

actual fun createHttpEngine(): HttpClientEngineFactory<*> = Js
