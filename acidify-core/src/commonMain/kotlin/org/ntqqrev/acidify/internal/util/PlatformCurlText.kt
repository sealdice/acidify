package org.ntqqrev.acidify.internal.util

internal data class PlatformCurlTextResponse(
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val body: String,
) {
    fun header(name: String): String? = headers[name.lowercase()]?.lastOrNull()
}

internal expect fun platformCurlTextRequestOrNull(
    method: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    body: String? = null,
    contentType: String? = null,
    followRedirects: Boolean = true,
    proxy: String? = null,
): PlatformCurlTextResponse?
