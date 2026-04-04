@file:JvmName("Main")

package org.ntqqrev.yogurt

import com.github.ajalt.mordant.rendering.TextColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.ntqqrev.acidify.codec.calculatePcmDuration
import org.ntqqrev.acidify.codec.getImageInfo
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
        "codec", "codec-decode" -> {
            val inputPath = args.getOrNull(1)
                ?: return failCli("Usage: yogurt codec <input-audio-path> [output-pcm-path]")
            val outputPath = args.getOrNull(2) ?: defaultCodecOutputPath(inputPath)

            println("[codec] reading: $inputPath")
            val input = readByteArrayFromFilePath(inputPath)
            println("[codec] bytes: ${input.size}")
            println("[codec] calling audioToPcm")
            val pcm = runBlocking {
                FFMpegCodec.audioToPcm(input)
            }
            println("[codec] pcm bytes: ${pcm.size}")
            println("[codec] writing: $outputPath")
            SystemFileSystem.sink(Path(outputPath)).buffered().use { sink ->
                sink.write(pcm)
            }
            println(outputPath)
            return true
        }

        "codec-stat" -> {
            val inputPath = args.getOrNull(1)
                ?: return failCli("Usage: yogurt codec-stat <input-path>")
            val input = readByteArrayFromFilePath(inputPath)
            println("path=$inputPath")
            println("bytes=${input.size}")
            println("head16=${input.take(16).joinToString(" ") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }}")
            return true
        }

        "codec-pcm-duration" -> {
            val inputPath = args.getOrNull(1)
                ?: return failCli("Usage: yogurt codec-pcm-duration <pcm-path>")
            val input = readByteArrayFromFilePath(inputPath)
            println(calculatePcmDuration(input))
            return true
        }

        "codec-image-info" -> {
            val inputPath = args.getOrNull(1)
                ?: return failCli("Usage: yogurt codec-image-info <image-path>")
            val input = readByteArrayFromFilePath(inputPath)
            val info = getImageInfo(input)
            println("format=${info.format}")
            println("width=${info.width}")
            println("height=${info.height}")
            return true
        }

        "codec-video-info" -> {
            val inputPath = args.getOrNull(1)
                ?: return failCli("Usage: yogurt codec-video-info <video-path>")
            val input = readByteArrayFromFilePath(inputPath)
            println("[codec-video-info] bytes: ${input.size}")
            println("[codec-video-info] calling getVideoInfo")
            val info = runBlocking {
                FFMpegCodec.getVideoInfo(input)
            }
            println("width=${info.width}")
            println("height=${info.height}")
            println("duration=${info.duration}")
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
