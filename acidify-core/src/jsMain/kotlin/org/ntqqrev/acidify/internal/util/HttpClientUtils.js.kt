package org.ntqqrev.acidify.internal.util

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

internal actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(block)
