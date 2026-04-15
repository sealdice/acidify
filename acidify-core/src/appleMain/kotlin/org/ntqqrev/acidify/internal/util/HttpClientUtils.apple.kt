package org.ntqqrev.acidify.internal.util

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

internal actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Darwin, block)
