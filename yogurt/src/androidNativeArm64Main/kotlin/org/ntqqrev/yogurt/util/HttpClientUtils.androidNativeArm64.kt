package org.ntqqrev.yogurt.util

import io.ktor.client.HttpClient
import org.ntqqrev.androidhttps.createAndroidNativePlatformHttpClient

internal actual fun createPlatformHttpClient(): HttpClient = createAndroidNativePlatformHttpClient()
