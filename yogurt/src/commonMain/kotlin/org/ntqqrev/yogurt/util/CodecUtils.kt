package org.ntqqrev.yogurt.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import org.ntqqrev.acidify.milky.Codec
import org.ntqqrev.acidify.milky.VideoInfo
import org.ntqqrev.yogurt.YogurtApp.config
import org.ntqqrev.yogurt.fs.FileSystem
import org.ntqqrev.yogurt.fs.withFs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object FFmpegCodec : Codec {
    val ffmpegMutex = Mutex()
    private val ffmpegCommand: String? by lazy { discoverFfmpegCommand(config.milky.ffmpegPath) }

    override suspend fun getImageInfo(input: ByteArray) = ffmpegCommand?.let { ffmpeg ->
        withContext(Dispatchers.IO) {
            withTempFile(input, "image-input") { inputPath ->
                val result = executeCommand(
                    ffmpeg,
                    "-hide_banner",
                    "-nostdin",
                    "-i",
                    inputPath,
                    "-frames:v",
                    "1",
                    "-f",
                    "null",
                    "-"
                )
                parseImageInfoFromFfmpegOutput(result.stderr)
                    ?: codecGetImageInfo(input)
            }
        }
    } ?: codecGetImageInfo(input)

    override suspend fun audioToPcm(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        ffmpegCommand?.let { ffmpeg ->
            return@withLock withContext(Dispatchers.IO) {
                withTempFile(input, "audio-input") { inputPath ->
                    val outputPath = createCodecTempFilePath("audio-pcm", ".pcm")
                    try {
                        val result = executeCommand(
                            ffmpeg,
                            "-hide_banner",
                            "-nostdin",
                            "-y",
                            "-i",
                            inputPath,
                            "-vn",
                            "-sn",
                            "-f",
                            "s16le",
                            "-acodec",
                            "pcm_s16le",
                            "-ac",
                            "1",
                            "-ar",
                            "24000",
                            outputPath,
                        )
                        if (result.errorCode == 0) {
                            return@withTempFile Path(outputPath).readBytes()
                        }
                    } finally {
                        deleteCodecTempFile(outputPath)
                    }
                    codecAudioToPcm(input)
                }
            }
        }
        codecAudioToPcm(input)
    }

    override suspend fun silkEncode(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        codecSilkEncode(input)
    }

    override suspend fun calculatePcmDuration(
        input: ByteArray,
        bitDepth: Int,
        channelCount: Int,
        sampleRate: Int,
    ): Duration {
        val bytesPerSample = bitDepth / 8.0
        if (bytesPerSample <= 0.0 || channelCount <= 0 || sampleRate <= 0) {
            return Duration.ZERO
        }

        val frameCount = input.size / (bytesPerSample * channelCount)
        return (frameCount / sampleRate).seconds
    }

    override suspend fun getVideoInfo(videoData: ByteArray): VideoInfo = if (ffmpegCommand != null) {
        withContext(Dispatchers.IO) {
            withTempFile(videoData, "video-input") { inputPath ->
                val result = executeCommand(
                    ffmpegCommand!!,
                    "-hide_banner",
                    "-nostdin",
                    "-i",
                    inputPath,
                    "-frames:v",
                    "1",
                    "-f",
                    "null",
                    "-"
                )

                parseVideoInfoFromFfmpegOutput(result.stderr)
                    ?: throw IllegalStateException(
                        "Failed to parse video info from ffmpeg output (code=${result.errorCode}): ${result.stderr}"
                    )
            }
        }
    } else {
        ffmpegMutex.withLock {
            codecGetVideoInfo(videoData)
        }
    }

    override suspend fun getVideoFirstFrameJpg(videoData: ByteArray): ByteArray = if (ffmpegCommand != null) {
        withContext(Dispatchers.IO) {
            withTempFile(videoData, "video-input") { inputPath ->
                val outputPath = createCodecTempFilePath("video-first-frame", ".jpg")
                try {
                    val result = executeCommand(
                        ffmpegCommand!!,
                        "-hide_banner",
                        "-nostdin",
                        "-y",
                        "-i",
                        inputPath,
                        "-an",
                        "-sn",
                        "-frames:v",
                        "1",
                        outputPath
                    )

                    if (result.errorCode != 0) {
                        throw IllegalStateException(
                            "ffmpeg failed to extract the first video frame (code=${result.errorCode}): ${result.stderr}"
                        )
                    }

                    Path(outputPath).readBytes()
                } finally {
                    deleteCodecTempFile(outputPath)
                }
            }
        }
    } else {
        ffmpegMutex.withLock {
            codecGetVideoFirstFrameJpg(videoData)
        }
    }
}

private inline fun <T> withTempFile(
    data: ByteArray,
    kind: String,
    block: FileSystem.(String) -> T,
): T = withFs {
    val inputPath = createCodecTempFilePath(kind)
    try {
        Path(inputPath).write(data)
        return block(inputPath)
    } finally {
        deleteCodecTempFile(inputPath)
    }
}

private fun createCodecTempFilePath(kind: String, extension: String = ".tmp"): String = withFs {
    val basePath = createCommandTempFilePath(kind)
    if (extension == ".tmp") {
        return basePath
    }

    val targetPath = basePath.removeSuffix(".tmp") + extension
    atomicMove(Path(basePath), Path(targetPath))
    return targetPath
}

private fun deleteCodecTempFile(path: String) = withFs {
    val file = Path(path)
    if (exists(file)) {
        delete(file, mustExist = false)
    }
}

private fun parseVideoInfoFromFfmpegOutput(output: String): VideoInfo? {
    val duration = durationRegex.find(output)?.let { match ->
        parseFfmpegDuration(match.groupValues[1])
    } ?: return null

    val videoLine = output.lineSequence().firstOrNull { "Video:" in it } ?: return null
    val resolution = resolutionRegex.find(videoLine) ?: return null

    return VideoInfo(
        width = resolution.groupValues[1].toInt(),
        height = resolution.groupValues[2].toInt(),
        duration = duration,
    )
}

private fun parseFfmpegDuration(raw: String): Duration {
    val parts = raw.split(":")
    require(parts.size == 3) { "Unsupported ffmpeg duration format: $raw" }

    val hoursPart = parts[0].toInt()
    val minutesPart = parts[1].toInt()
    val secondsPart = parts[2].toDouble()

    return hoursPart.hours + minutesPart.minutes + secondsPart.seconds
}

private val durationRegex = Regex("""Duration:\s*([0-9]{2}:[0-9]{2}:[0-9]{2}(?:\.[0-9]+)?)""")
private val resolutionRegex = Regex("""\b(\d{2,5})x(\d{2,5})\b""")


private fun parseImageInfoFromFfmpegOutput(output: String): org.ntqqrev.acidify.milky.ImageInfo? {
    val videoLine = output.lineSequence().firstOrNull { "Video:" in it } ?: return null
    val resolution = resolutionRegex.find(videoLine) ?: return null
    val format = when {
        "png" in videoLine.lowercase() -> org.ntqqrev.acidify.message.ImageFormat.PNG
        "gif" in videoLine.lowercase() -> org.ntqqrev.acidify.message.ImageFormat.GIF
        "mjpeg" in videoLine.lowercase() || "jpeg" in videoLine.lowercase() || "jpg" in videoLine.lowercase() -> org.ntqqrev.acidify.message.ImageFormat.JPEG
        "bmp" in videoLine.lowercase() -> org.ntqqrev.acidify.message.ImageFormat.BMP
        "webp" in videoLine.lowercase() -> org.ntqqrev.acidify.message.ImageFormat.WEBP
        "tiff" in videoLine.lowercase() -> org.ntqqrev.acidify.message.ImageFormat.TIFF
        else -> return null
    }

    return org.ntqqrev.acidify.milky.ImageInfo(
        format = format,
        width = resolution.groupValues[1].toInt(),
        height = resolution.groupValues[2].toInt(),
    )
}
