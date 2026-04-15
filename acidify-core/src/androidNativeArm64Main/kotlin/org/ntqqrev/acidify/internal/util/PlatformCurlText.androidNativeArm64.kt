package org.ntqqrev.acidify.internal.util

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.curl.Curl
import io.ktor.client.engine.curl.defaultAndroidNativeCurlCaInfoPathOrNull
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.runBlocking

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

    val client = HttpClient(Curl) {
        followRedirects = followRedirects
        engine {
            caInfo = defaultAndroidNativeCurlCaInfoPathOrNull()
        }
        install(HttpRequestRetry) {
            maxRetries = 0
        }
    }

    return try {
        runBlocking {
            val response = client.request(url) {
                this.method = HttpMethod.parse(method)
                headers.forEach { (key, value) -> header(key, value) }
                if (!contentType.isNullOrBlank()) {
                    header(HttpHeaders.ContentType, contentType)
                }
                if (body != null) {
                    setBody(body)
                }
            }
            PlatformCurlTextResponse(
                statusCode = response.status.value,
                headers = response.headers.entries().associate { (key, values) -> key.lowercase() to values },
                body = response.body(),
            )
        }
    } finally {
        client.close()
    }
}
