@file:OptIn(ExperimentalForeignApi::class)

package org.ntqqrev.yogurt.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.*
import platform.posix.errno
import platform.posix.strerror
import platform.posix.system

actual fun executeCommand(vararg args: String): CommandExecutionResult {
    require(args.isNotEmpty()) { "Command arguments must not be empty." }

    val stdoutPath = createCommandTempFilePath("stdout")
    val stderrPath = createCommandTempFilePath("stderr")

    try {
        val status = system(buildPosixRedirectedCommand(args, stdoutPath, stderrPath))
        if (status < 0) {
            return CommandExecutionResult(-1, "", strerror(errno)?.toKString() ?: "Failed to execute command.")
        }

        return CommandExecutionResult(
            errorCode = exitCodeFromStatus(status),
            stdout = readCommandTempFile(stdoutPath),
            stderr = readCommandTempFile(stderrPath),
        )
    } finally {
        deleteCommandTempFile(stdoutPath)
        deleteCommandTempFile(stderrPath)
    }
}

private fun exitCodeFromStatus(status: Int): Int =
    when {
        (status and 0x7f) == 0 -> (status ushr 8) and 0xff
        (status and 0x7f) != 0x7f -> 128 + (status and 0x7f)
        else -> -1
    }


actual fun currentProgramDirectory(): String? = memScoped {
    val bufferSize = 4096
    val buffer = allocArray<ByteVar>(bufferSize)
    val length = platform.posix.readlink("/proc/self/exe", buffer, (bufferSize - 1).convert())
    if (length <= 0) return null
    buffer[length] = 0
    buffer.toKString().substringBeforeLast('/', "").ifBlank { null }
}
