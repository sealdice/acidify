package org.ntqqrev.yogurt.util

import org.ntqqrev.yogurt.fs.withFs
import java.io.File

actual fun executeCommand(vararg args: String): CommandExecutionResult = withFs {
    withCommandTempFile { stdout, stderr ->
        require(args.isNotEmpty()) { "Command arguments must not be empty." }

        try {
            val process = ProcessBuilder(args.toList())
                .redirectOutput(File(stdout.toString()))
                .redirectError(File(stderr.toString()))
                .start()

            CommandExecutionResult(
                errorCode = process.waitFor(),
                stdout = stdout.readText(),
                stderr = stderr.readText(),
            )
        } catch (e: Exception) {
            CommandExecutionResult(
                errorCode = -1,
                stdout = "",
                stderr = e.message ?: e.toString(),
            )
        }
    }
}


actual fun currentProgramDirectory(): String? = runCatching {
    val location = object {}.javaClass.protectionDomain.codeSource?.location?.toURI() ?: return null
    val file = File(location)
    (if (file.isDirectory) file else file.parentFile)?.absolutePath
}.getOrNull() ?: runCatching { File(".").absoluteFile.canonicalPath }.getOrNull()
