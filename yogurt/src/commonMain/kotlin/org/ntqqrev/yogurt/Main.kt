@file:JvmName("Main")

package org.ntqqrev.yogurt

import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.ntqqrev.yogurt.YogurtApp.config
import org.ntqqrev.yogurt.YogurtApp.t
import org.ntqqrev.yogurt.util.FFMpegCodec
import org.ntqqrev.yogurt.util.isCausedByAddrInUse
import org.ntqqrev.yogurt.util.readByteArrayFromFilePath
import org.ntqqrev.yogurt.util.readEnvironmentVariableCompat
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.milliseconds

fun main(args: Array<String>) {
    if (runCliCommand(args)) {
        return
    }

    runServer()
}

private fun runServer() {
    val server = YogurtApp.createServer()
    try {
        server.start(wait = false)
        server.onSigint {
            server.stop(gracePeriodMillis = 2000L, timeoutMillis = 5000L)
        }
        runBlocking {
            delay(Long.MAX_VALUE.milliseconds)
        }
    } catch (e: Throwable) {
        if (e.isCausedByAddrInUse()) {
            t.println(
                TextColors.red(
                    """
                        无法启动服务器，可能是 ${config.milky.http.host}:${config.milky.http.port} 已被占用。
                        请检查是否有其他程序正在使用该地址，或者修改配置文件中的 host 和 port 后重试。
                    """.trimIndent()
                )
            )
        }
        throw e
    }
}

private fun runCliCommand(args: Array<String>): Boolean {
    if (args.isEmpty()) {
        return false
    }

    when (args[0]) {
        "codec" -> {
            val inputPath = args.getOrNull(1)
                ?: return failCli("Usage: yogurt codec <input-audio-path> [output-pcm-path]")
            val outputPath = args.getOrNull(2) ?: defaultCodecOutputPath(inputPath)
            val input = readByteArrayFromFilePath(inputPath)
            val pcm = runBlocking {
                FFMpegCodec.audioToPcm(input)
            }
            SystemFileSystem.sink(Path(outputPath)).buffered().use { sink ->
                sink.write(pcm)
            }
            println(outputPath)
            return true
        }

        "env" -> {
            val name = args.getOrNull(1)
                ?: return failCli("Usage: yogurt env <ENV_NAME>")
            println(readEnvironmentVariableCompat(name).orEmpty())
            return true
        }

        else -> return false
    }
}

private fun defaultCodecOutputPath(inputPath: String): String {
    val separatorIndex = maxOf(inputPath.lastIndexOf('/'), inputPath.lastIndexOf('\\'))
    val extensionIndex = inputPath.lastIndexOf('.')
    return if (extensionIndex > separatorIndex) {
        inputPath.substring(0, extensionIndex) + ".pcm"
    } else {
        "$inputPath.pcm"
    }
}

private fun failCli(message: String): Boolean {
    println(message)
    halt(1)
    return true
}
