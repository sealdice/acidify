package org.ntqqrev.yogurt.util

import org.ntqqrev.androidhttps.executeTextRequest

actual fun platformHttpsSmokeTestOrNull(url: String): PlatformHttpsSmokeTestResponse? {
    val response = executeTextRequest(
        method = "GET",
        url = url,
        followRedirects = true,
    )
    return PlatformHttpsSmokeTestResponse(
        statusCode = response.statusCode,
        body = response.body,
    )
}
