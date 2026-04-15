package org.ntqqrev.acidify.internal.util

import io.ktor.client.HttpClient

internal expect fun createPlatformHttpClient(): HttpClient
