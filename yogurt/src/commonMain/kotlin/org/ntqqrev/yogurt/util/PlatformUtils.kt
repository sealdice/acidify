package org.ntqqrev.yogurt.util

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory
import org.ntqqrev.yogurt.fs.withFs
import kotlin.random.Random

data class CommandExecutionResult(
    val errorCode: Int,
    val stdout: String,
    val stderr: String,
)

expect fun executeCommand(vararg args: String): CommandExecutionResult

expect fun currentProgramDirectory(): String?

fun createCommandTempFilePath(kind: String): String {
    while (true) {
        withFs {
            val candidate = Path(
                SystemTemporaryDirectory,
                "yogurt-$kind-${Random.nextLong().toULong().toString(16)}.tmp",
            )
            if (exists(candidate)) {
                continue
            }

            sink(candidate).buffered().use { }
            return candidate.toString()
        }
    }
}

fun readCommandTempFile(path: String): String = withFs {
    val file = Path(path)
    if (!exists(file)) {
        return ""
    }

    return Path(path).readText()
}

fun deleteCommandTempFile(path: String) = withFs {
    val file = Path(path)
    if (exists(file)) {
        delete(file, mustExist = false)
    }
}

fun buildPosixRedirectedCommand(
    args: Array<out String>,
    stdoutPath: String,
    stderrPath: String,
): String =
    buildString {
        append(args.joinToString(" ") { quotePosixArgument(it) })
        append(" > ")
        append(quotePosixArgument(stdoutPath))
        append(" 2> ")
        append(quotePosixArgument(stderrPath))
    }

private fun quotePosixArgument(argument: String): String =
    "'" + argument.replace("'", "'\"'\"'") + "'"


fun discoverFfmpegCommand(configuredPath: String): String? {
    if (configuredPath.isNotBlank()) {
        return configuredPath
    }

    val candidateNames = listOf("ffmpeg", "ffmpeg.exe")
    val candidates = LinkedHashSet<String>()

    currentProgramDirectory()?.let { programDir ->
        withFs {
            for (name in candidateNames) {
                val candidate = Path(programDir, name)
                if (exists(candidate)) {
                    candidates += candidate.toString()
                }
            }
        }
    }

    withFs {
        val cwd = resolve(Path("."))
        for (name in candidateNames) {
            val candidate = Path(cwd, name)
            if (exists(candidate)) {
                candidates += candidate.toString()
            }
        }
    }

    candidates += candidateNames

    return candidates.firstOrNull(::isFfmpegUsable)
}

private fun isFfmpegUsable(command: String): Boolean {
    val result = executeCommand(command, "-version")
    return result.errorCode == 0 && listOf(result.stdout, result.stderr).any { "ffmpeg version" in it }
}
