package org.ntqqrev.acidify.internal.util

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.java.Java

internal actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Java, block)
