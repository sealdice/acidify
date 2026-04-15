package org.ntqqrev.acidify.internal.util

internal actual fun platformCurlTextRequestOrNull(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String?,
    contentType: String?,
    followRedirects: Boolean,
    proxy: String?,
): PlatformCurlTextResponse? = null
