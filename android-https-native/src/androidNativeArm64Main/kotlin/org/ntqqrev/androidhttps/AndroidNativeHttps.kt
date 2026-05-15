@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.ntqqrev.androidhttps

import io.ktor.http.Url
import kotlinx.cinterop.*
import org.ntqqrev.androidhttps.native.*
import platform.posix.F_OK
import platform.posix.access
import platform.posix.readlink

data class AndroidNativeBinaryResponse(
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val body: ByteArray,
) {
    fun header(name: String): String? = headers[name.lowercase()]?.lastOrNull()
}

data class AndroidNativeTextResponse(
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val body: String,
) {
    fun header(name: String): String? = headers[name.lowercase()]?.lastOrNull()
}

fun defaultCaBundlePathOrNull(): String? {
    return currentProgramDirectory()
        ?.let { "$it/cacert.pem" }
        ?.takeIf(::fileExists)
}

fun executeTextRequest(
    method: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    body: String? = null,
    contentType: String? = null,
    followRedirects: Boolean = true,
    maxRedirects: Int = 5,
    timeoutMillis: Int = 30_000,
    caBundlePath: String? = defaultCaBundlePathOrNull(),
): AndroidNativeTextResponse = executeBinaryRequest(
    method = method,
    url = url,
    headers = headers,
    body = body?.encodeToByteArray(),
    contentType = contentType,
    followRedirects = followRedirects,
    maxRedirects = maxRedirects,
    timeoutMillis = timeoutMillis,
    caBundlePath = caBundlePath,
).let {
    AndroidNativeTextResponse(
        statusCode = it.statusCode,
        headers = it.headers,
        body = it.body.decodeToString(),
    )
}

fun executeBinaryRequest(
    method: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    body: ByteArray? = null,
    contentType: String? = null,
    followRedirects: Boolean = true,
    maxRedirects: Int = 5,
    timeoutMillis: Int = 30_000,
    caBundlePath: String? = defaultCaBundlePathOrNull(),
): AndroidNativeBinaryResponse {
    var currentMethod = method.uppercase()
    var currentUrl = url
    var currentBody = body
    var currentContentType = contentType

    repeat(maxRedirects + 1) { redirectCount ->
        val response = executeBinaryRequestOnce(
            method = currentMethod,
            url = currentUrl,
            headers = headers,
            body = currentBody,
            contentType = currentContentType,
            timeoutMillis = timeoutMillis,
            caBundlePath = caBundlePath,
        )
        if (!followRedirects || response.statusCode !in setOf(301, 302, 303, 307, 308)) {
            return response
        }
        val location = response.header("location")
            ?: return response
        if (redirectCount == maxRedirects) {
            throw IllegalStateException("Too many redirects while requesting $url")
        }
        currentUrl = resolveRedirectUrl(currentUrl, location)
        if (response.statusCode == 303) {
            currentMethod = "GET"
            currentBody = null
            currentContentType = null
        }
    }
    throw IllegalStateException("Unreachable redirect state for $url")
}

private fun executeBinaryRequestOnce(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: ByteArray?,
    contentType: String?,
    timeoutMillis: Int,
    caBundlePath: String?,
): AndroidNativeBinaryResponse = memScoped {
    val headerEntries = headers.entries.toList()
    val nativeHeaders = if (headerEntries.isEmpty()) null else allocArray<acidify_http_header>(headerEntries.size)
    headerEntries.forEachIndexed { index, entry ->
        nativeHeaders!![index].name = entry.key.cstr.getPointer(this)
        nativeHeaders[index].value = entry.value.cstr.getPointer(this)
    }

    val request = alloc<acidify_http_request>().apply {
        this.method = method.cstr.getPointer(this@memScoped)
        this.url = url.cstr.getPointer(this@memScoped)
        this.headers = nativeHeaders
        this.header_count = headerEntries.size.convert()
        this.body = null
        this.body_len = 0.convert()
        this.content_type = contentType?.cstr?.getPointer(this@memScoped)
        this.timeout_ms = timeoutMillis
        this.ca_bundle_path = if (url.startsWith("https://", ignoreCase = true)) {
            caBundlePath?.cstr?.getPointer(this@memScoped)
        } else {
            null
        }
    }
    val response = alloc<acidify_http_response>().apply {
        raw_response = null
        raw_response_len = 0.convert()
        error_message = null
    }

    try {
        val payload = body?.takeIf { it.isNotEmpty() }
        payload?.usePinned {
            request.body = it.addressOf(0).reinterpret()
            request.body_len = payload.size.convert()
            executeOrThrow(request.ptr, response.ptr)
        } ?: executeOrThrow(request.ptr, response.ptr)

        val rawLength = response.raw_response_len.toLong()
        require(rawLength <= Int.MAX_VALUE.toLong()) { "HTTP response is too large: $rawLength bytes" }

        val rawBytes = response.raw_response
            ?.reinterpret<ByteVar>()
            ?.readBytes(rawLength.toInt())
            ?: ByteArray(0)
        parseRawHttpResponse(rawBytes)
    } finally {
        acidify_http_response_free(response.ptr)
    }
}

private fun executeOrThrow(
    request: CPointer<acidify_http_request>,
    response: CPointer<acidify_http_response>,
) {
    val result = acidify_http_execute(request, response)
    if (result != 0) {
        val error = response.pointed.error_message?.toKString()
        throw IllegalStateException(error ?: "Android Native HTTPS request failed with code $result")
    }
}

private fun parseRawHttpResponse(rawBytes: ByteArray): AndroidNativeBinaryResponse {
    val headerEnd = rawBytes.indexOfSequence(byteArrayOf('\r'.code.toByte(), '\n'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte()))
    require(headerEnd >= 0) { "Malformed HTTP response: missing header separator" }
    val headerText = rawBytes.copyOfRange(0, headerEnd).decodeToString()
    val bodyStart = headerEnd + 4
    val rawBody = rawBytes.copyOfRange(bodyStart, rawBytes.size)

    val lines = headerText.split("\r\n")
    require(lines.isNotEmpty()) { "Malformed HTTP response: missing status line" }
    val statusCode = lines.first().split(' ').getOrNull(1)?.toIntOrNull()
        ?: throw IllegalStateException("Malformed HTTP status line: ${lines.first()}")
    val headers = linkedMapOf<String, MutableList<String>>()
    lines.drop(1).forEach { line ->
        val separatorIndex = line.indexOf(':')
        if (separatorIndex <= 0) return@forEach
        val name = line.substring(0, separatorIndex).trim().lowercase()
        val value = line.substring(separatorIndex + 1).trim()
        headers.getOrPut(name) { mutableListOf() }.add(value)
    }

    val body = when {
        headers["transfer-encoding"]?.any { it.contains("chunked", ignoreCase = true) } == true -> decodeChunkedBody(rawBody)
        headers["content-length"]?.lastOrNull()?.toIntOrNull() != null -> {
            val length = headers["content-length"]!!.last().toInt()
            rawBody.copyOfRange(0, minOf(length, rawBody.size))
        }
        else -> rawBody
    }

    return AndroidNativeBinaryResponse(
        statusCode = statusCode,
        headers = headers,
        body = body,
    )
}

private fun decodeChunkedBody(rawBody: ByteArray): ByteArray {
    val output = ArrayList<Byte>()
    var cursor = 0
    while (cursor < rawBody.size) {
        val lineEnd = rawBody.indexOfSequence(byteArrayOf('\r'.code.toByte(), '\n'.code.toByte()), cursor)
        require(lineEnd >= 0) { "Malformed chunked response: missing chunk size line ending" }
        val sizeToken = rawBody.copyOfRange(cursor, lineEnd).decodeToString().substringBefore(';').trim()
        val chunkSize = sizeToken.toInt(16)
        cursor = lineEnd + 2
        if (chunkSize == 0) {
            break
        }
        val chunkEnd = cursor + chunkSize
        require(chunkEnd <= rawBody.size) { "Malformed chunked response: chunk exceeds body length" }
        rawBody.copyOfRange(cursor, chunkEnd).forEach(output::add)
        cursor = chunkEnd + 2
    }
    return output.toByteArray()
}

private fun ByteArray.indexOfSequence(sequence: ByteArray, startIndex: Int = 0): Int {
    if (size < sequence.size || startIndex > size - sequence.size) {
        return -1
    }
    outer@ for (index in startIndex..size - sequence.size) {
        for (offset in sequence.indices) {
            if (this[index + offset] != sequence[offset]) {
                continue@outer
            }
        }
        return index
    }
    return -1
}

private fun resolveRedirectUrl(currentUrl: String, location: String): String {
    if (location.startsWith("http://") || location.startsWith("https://")) {
        return location
    }
    val current = Url(currentUrl)
    val authority = buildString {
        append(current.protocol.name)
        append("://")
        append(current.host)
        if (current.port != current.protocol.defaultPort) {
            append(':')
            append(current.port)
        }
    }
    return when {
        location.startsWith("//") -> "${current.protocol.name}:$location"
        location.startsWith("/") -> authority + location
        location.startsWith("?") -> authority + current.encodedPath + location
        else -> {
            val basePath = current.encodedPath.substringBeforeLast('/', "")
            authority + if (basePath.isEmpty()) "/$location" else "$basePath/$location"
        }
    }
}

private fun fileExists(path: String): Boolean = access(path, F_OK) == 0

private fun currentProgramDirectory(): String? = memScoped {
    val bufferSize = 4096
    val buffer = allocArray<ByteVar>(bufferSize)
    val length = readlink("/proc/self/exe", buffer, (bufferSize - 1).convert())
    if (length <= 0) return@memScoped null
    buffer[length.toInt()] = 0
    buffer.toKString().substringBeforeLast('/', "").ifBlank { null }
}
