package org.ntqqrev.acidify.internal.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.engine.curl.defaultAndroidNativeCurlCaInfoPathOrNull

internal actual fun createPlatformHttpClient(): HttpClient = HttpClient(Curl) {
    engine {
        caInfo = defaultAndroidNativeCurlCaInfoPathOrNull()
    }
}
