package org.ntqqrev.acidify.internal.util

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import org.ntqqrev.androidhttps.createAndroidNativePlatformHttpClient

internal actual fun createPlatformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    createAndroidNativePlatformHttpClient(block)
