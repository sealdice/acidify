@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.ktor.client.engine.curl

import io.ktor.client.engine.curl.internal.EMBEDDED_ANDROID_NATIVE_CA_PEM
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.F_OK
import platform.posix.access
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.getenv
import platform.posix.readlink

private const val EMBEDDED_CA_FILENAME = ".acidify-curl-ca.pem"

private var cachedEmbeddedCaPath: String? = null

public fun defaultAndroidNativeCurlCaInfoPathOrNull(): String? {
    currentProgramDirectory()
        ?.let { "$it/cacert.pem" }
        ?.takeIf(::fileExists)
        ?.let { return it }

    cachedEmbeddedCaPath?.takeIf(::fileExists)?.let { return it }

    val targetDirectory = preferredWritableDirectory() ?: return null
    val targetPath = "$targetDirectory/$EMBEDDED_CA_FILENAME"
    if (!fileExists(targetPath)) {
        writeTextFile(targetPath, EMBEDDED_ANDROID_NATIVE_CA_PEM)
    }
    cachedEmbeddedCaPath = targetPath
    return targetPath
}

private fun preferredWritableDirectory(): String? {
    getenv("TMPDIR")?.toKString()?.takeIf(::fileExists)?.let { return it }
    return "/data/local/tmp".takeIf(::fileExists)
}

private fun fileExists(path: String): Boolean = access(path, F_OK) == 0

private fun writeTextFile(path: String, text: String) {
    val file = fopen(path, "wb") ?: error("Failed to open $path for writing")
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

private fun currentProgramDirectory(): String? = memScoped {
    val bufferSize = 4096
    val buffer = allocArray<ByteVar>(bufferSize)
    val length = readlink("/proc/self/exe", buffer, (bufferSize - 1).convert())
    if (length <= 0) return@memScoped null
    buffer[length] = 0
    buffer.toKString().substringBeforeLast('/', "").ifBlank { null }
}
