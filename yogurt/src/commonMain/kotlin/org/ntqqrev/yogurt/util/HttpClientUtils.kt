package org.ntqqrev.yogurt.util

import io.ktor.client.HttpClient

internal expect fun createPlatformHttpClient(): HttpClient
