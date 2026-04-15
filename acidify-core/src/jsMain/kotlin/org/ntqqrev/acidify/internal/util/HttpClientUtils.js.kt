package org.ntqqrev.acidify.internal.util

import io.ktor.client.HttpClient

internal actual fun createPlatformHttpClient(): HttpClient = HttpClient()
