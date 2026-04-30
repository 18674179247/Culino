package com.culino.core.network

import io.ktor.client.engine.*

expect fun createHttpEngine(): HttpClientEngineFactory<*>
