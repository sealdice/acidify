@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.ntqqrev.acidify.internal.util

import kotlinx.cinterop.*
import platform.posix.X_OK
import platform.posix.access
import platform.posix.errno
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite
import platform.posix.getenv
import platform.posix.readlink
import platform.posix.remove
import platform.posix.stat
import platform.posix.strerror
import platform.posix.system
import kotlin.random.Random

internal actual fun platformCurlTextRequestOrNull(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String?,
    contentType: String?,
    followRedirects: Boolean,
    proxy: String?,
): PlatformCurlTextResponse? {
    val curlBinary = discoverCurlBinary()
    val stdoutPath = createTempPath("curl-stdout")
    val stderrPath = createTempPath("curl-stderr")
    val headerPath = createTempPath("curl-headers")
    val bodyPath = createTempPath("curl-body")
    val inputPath = body?.let {
        createTempPath("curl-input").also { path -> writeTextFile(path, body) }
    }
    return try {
        val args = buildList {
            add(curlBinary)
            add("--silent")
            add("--show-error")
            add("--http1.1")
            add("--request")
            add(method)
            add("--dump-header")
            add(headerPath)
            add("--output")
            add(bodyPath)
            add("--write-out")
            add("%{http_code}")
            if (followRedirects) {
                add("--location")
            }
            if (!proxy.isNullOrBlank()) {
                add("--proxy")
                add(proxy)
            }
            if (!contentType.isNullOrBlank()) {
                add("--header")
                add("Content-Type: $contentType")
            }
            headers.forEach { (key, value) ->
                add("--header")
                add("$key: $value")
            }
            if (inputPath != null) {
                add("--data-binary")
                add("@$inputPath")
            }
            add(url)
        }
        val status = decodePosixStatus(system(buildRedirectedCommand(args, stdoutPath, stderrPath)))
        val stdout = readTextFile(stdoutPath).trim()
        val stderr = readTextFile(stderrPath).trim()
        if (status != 0) {
            val message = stderr.ifBlank { "curl exited with code $status" }
            throw IllegalStateException(message)
        }
        PlatformCurlTextResponse(
            statusCode = stdout.toIntOrNull() ?: -1,
            headers = parseHeaders(readTextFile(headerPath)),
            body = readTextFile(bodyPath),
        )
    } finally {
        listOf(stdoutPath, stderrPath, headerPath, bodyPath, inputPath)
            .filterNotNull()
            .forEach { path -> remove(path) }
    }
}

private fun discoverCurlBinary(): String {
    getenv("YOGURT_CURL_PATH")?.toKString()?.takeIf { access(it, X_OK) == 0 }?.let { return it }
    getenv("ACIDIFY_CURL_PATH")?.toKString()?.takeIf { access(it, X_OK) == 0 }?.let { return it }

    currentProgramDirectory()?.let { programDir ->
        val candidate = "$programDir/curl"
        if (access(candidate, X_OK) == 0) {
            return candidate
        }
    }

    if (access("./curl", X_OK) == 0) {
        return "./curl"
    }
    if (access("/system/bin/curl", X_OK) == 0) {
        return "/system/bin/curl"
    }
    return "curl"
}

private fun currentProgramDirectory(): String? = memScoped {
    val bufferSize = 4096
    val buffer = allocArray<ByteVar>(bufferSize)
    val length = readlink("/proc/self/exe", buffer, (bufferSize - 1).convert())
    if (length <= 0) return@memScoped null
    buffer[length] = 0
    buffer.toKString().substringBeforeLast('/', "").ifBlank { null }
}

private fun createTempPath(kind: String): String =
    "/data/local/tmp/acidify-$kind-${Random.nextLong().toULong().toString(16)}.tmp"

private fun buildRedirectedCommand(args: List<String>, stdoutPath: String, stderrPath: String): String =
    buildString {
        append(args.joinToString(" ") { quotePosixArgument(it) })
        append(" > ")
        append(quotePosixArgument(stdoutPath))
        append(" 2> ")
        append(quotePosixArgument(stderrPath))
    }

private fun quotePosixArgument(argument: String): String =
    "'" + argument.replace("'", "'\"'\"'") + "'"

private fun decodePosixStatus(status: Int): Int = when {
    status < 0 -> -1
    (status and 0x7f) == 0 -> (status ushr 8) and 0xff
    (status and 0x7f) != 0x7f -> 128 + (status and 0x7f)
    else -> -1
}

private fun writeTextFile(path: String, text: String) {
    val file = fopen(path, "wb") ?: error(strerror(errno)?.toKString() ?: "Failed to open $path")
    try {
        val bytes = text.encodeToByteArray()
        if (bytes.isNotEmpty()) {
            bytes.usePinned {
                fwrite(it.addressOf(0), 1.convert(), bytes.size.convert(), file)
            }
        }
    } finally {
        fclose(file)
    }
}

private fun readTextFile(path: String): String = memScoped {
    val st = alloc<stat>()
    if (platform.posix.stat(path, st.ptr) != 0) return@memScoped ""
    val size = st.st_size.toInt()
    if (size <= 0) return@memScoped ""
    val file = fopen(path, "rb") ?: return@memScoped ""
    try {
        val bytes = ByteArray(size)
        bytes.usePinned {
            fread(it.addressOf(0), 1.convert(), bytes.size.convert(), file)
        }
        bytes.decodeToString()
    } finally {
        fclose(file)
    }
}

private fun parseHeaders(rawHeaders: String): Map<String, List<String>> {
    val result = linkedMapOf<String, MutableList<String>>()
    rawHeaders.lineSequence().forEach { line ->
        val separatorIndex = line.indexOf(':')
        if (separatorIndex <= 0) return@forEach
        val key = line.substring(0, separatorIndex).trim().lowercase()
        val value = line.substring(separatorIndex + 1).trim()
        result.getOrPut(key) { mutableListOf() }.add(value)
    }
    return result
}
