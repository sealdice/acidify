package org.ntqqrev.acidify.internal.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

internal actual fun createPlatformHttpClient(): HttpClient = HttpClient(CIO)
