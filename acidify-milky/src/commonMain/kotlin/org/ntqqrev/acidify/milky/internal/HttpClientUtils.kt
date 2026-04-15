package org.ntqqrev.acidify.milky.internal

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

internal expect fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient
