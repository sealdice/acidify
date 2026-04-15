package org.ntqqrev.acidify.internal.util

import org.ntqqrev.androidhttps.executeTextRequest

internal actual fun platformCurlTextRequestOrNull(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String?,
    contentType: String?,
    followRedirects: Boolean,
    proxy: String?,
): PlatformCurlTextResponse? {
    if (!proxy.isNullOrBlank()) {
        return null
    }
    val response = executeTextRequest(
        method = method,
        url = url,
        headers = headers,
        body = body,
        contentType = contentType,
        followRedirects = followRedirects,
    )
    return PlatformCurlTextResponse(
        statusCode = response.statusCode,
        headers = response.headers,
        body = response.body,
    )
}
