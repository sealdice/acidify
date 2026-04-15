package org.ntqqrev.yogurt.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java

internal actual fun createPlatformHttpClient(): HttpClient = HttpClient(Java)
