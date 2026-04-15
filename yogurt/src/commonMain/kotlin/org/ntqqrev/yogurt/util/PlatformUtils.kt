package org.ntqqrev.yogurt.util

import kotlinx.io.files.Path
import org.ntqqrev.yogurt.fs.withFs

data class CommandExecutionResult(
    val errorCode: Int,
    val stdout: String,
    val stderr: String,
)

expect fun executeCommand(vararg args: String): CommandExecutionResult
expect fun currentProgramDirectory(): String?

inline fun <R> withCommandTempFile(
    block: (stdout: Path, stderr: Path) -> R,
): R {
    val stdout = createTempFile("stdout")
    val stderr = createTempFile("stderr")
    try {
        return block(stdout, stderr)
    } finally {
        withFs {
            delete(stdout, mustExist = false)
            delete(stderr, mustExist = false)
        }
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
