package org.ntqqrev.yogurt.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl

internal actual fun createPlatformHttpClient(): HttpClient = HttpClient(Curl)
