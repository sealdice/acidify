package org.ntqqrev.yogurt.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.winhttp.WinHttp

internal actual fun createPlatformHttpClient(): HttpClient = HttpClient(WinHttp)
