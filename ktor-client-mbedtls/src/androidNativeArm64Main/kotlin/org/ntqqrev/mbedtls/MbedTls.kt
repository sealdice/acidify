package org.ntqqrev.mbedtls

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.engines
import io.ktor.utils.io.InternalAPI

@OptIn(ExperimentalStdlibApi::class)
@Suppress("unused", "DEPRECATION")
@EagerInitialization
private val initHook = MbedTls

@OptIn(InternalAPI::class)
data object MbedTls : HttpClientEngineFactory<HttpClientEngineConfig> {
    init {
        engines.append(this)
    }

    override fun create(block: HttpClientEngineConfig.() -> Unit): HttpClientEngine {
        val config = HttpClientEngineConfig().apply(block)
        return MbedTlsEngine(config)
    }
}