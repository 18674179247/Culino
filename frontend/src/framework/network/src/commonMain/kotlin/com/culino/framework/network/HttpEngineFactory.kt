package com.culino.framework.network

import io.ktor.client.engine.*

expect fun createHttpEngine(): HttpClientEngineFactory<*>
