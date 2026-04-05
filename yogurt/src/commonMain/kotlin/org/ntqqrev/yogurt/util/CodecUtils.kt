package org.ntqqrev.yogurt.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.files.Path
import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.message.ImageFormat
import org.ntqqrev.acidify.milky.*
import org.ntqqrev.yogurt.YogurtApp.config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object FFmpegCodec : Codec {
    val ffmpegMutex = Mutex()
    private val ffmpegCommand: String? by lazy { discoverFfmpegCommand(config.milky.ffmpegPath) }

    override suspend fun getImageInfo(input: ByteArray): ImageInfo = ffmpegCommand?.let { ffmpeg ->
        withContext(Dispatchers.IO) {
            val inputSource = TempFileMediaSource(
                kind = "ffmpeg-image-input",
                initialRawSource = Buffer().apply { write(input) },
            )
            try {
                val result = executeCommand(
                    ffmpeg,
                    "-hide_banner",
                    "-nostdin",
                    "-i",
                    inputSource.path.toString(),
                    "-frames:v",
                    "1",
                    "-f",
                    "null",
                    "-"
                )
                parseImageInfoFromFfmpegOutput(result.stderr) ?: codecGetImageInfo(input)
            } finally {
                inputSource.dispose()
            }
        }
    } ?: codecGetImageInfo(input)

    override suspend fun audioToPcm(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        ffmpegCommand?.let { ffmpeg ->
            return@withLock withContext(Dispatchers.IO) {
                val inputSource = TempFileMediaSource(
                    kind = "ffmpeg-audio-input",
                    initialRawSource = Buffer().apply { write(input) },
                )
                val outputSource = TempFileMediaSource(kind = "ffmpeg-audio-output", ext = "pcm")
                try {
                    val result = executeCommand(
                        ffmpeg,
                        "-hide_banner",
                        "-nostdin",
                        "-y",
                        "-i",
                        inputSource.path.toString(),
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
                        outputSource.path.toString(),
                    )
                    if (result.errorCode == 0) outputSource.readByteArray() else codecAudioToPcm(input)
                } finally {
                    inputSource.dispose()
                    outputSource.dispose()
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

    context(scope: MediaSourceScope)
    override suspend fun getVideoInfo(videoSource: MediaSource): VideoInfo = if (ffmpegCommand == null) {
        ffmpegMutex.withLock { codecGetVideoInfo(videoSource) }
    } else {
        withContext(Dispatchers.IO) {
            val result = executeCommand(
                ffmpegCommand!!,
                "-hide_banner",
                "-nostdin",
                "-i",
                videoSource.ensureLocalized().toString(),
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

    context(scope: MediaSourceScope)
    override suspend fun getVideoFirstFrameJpg(videoSource: MediaSource): ByteArray = if (ffmpegCommand == null) {
        ffmpegMutex.withLock { codecGetVideoFirstFrameJpg(videoSource) }
    } else {
        withContext(Dispatchers.IO) {
            val outputSource = tracked {
                TempFileMediaSource(kind = "ffmpeg-output", ext = "jpg")
            }
            val result = executeCommand(
                ffmpegCommand!!,
                "-hide_banner",
                "-nostdin",
                "-y",
                "-i",
                videoSource.ensureLocalized().toString(),
                "-an",
                "-sn",
                "-frames:v",
                "1",
                outputSource.path.toString()
            )
            if (result.errorCode != 0) {
                throw IllegalStateException(
                    "ffmpeg failed to extract the first video frame (code=${result.errorCode}): ${result.stderr}"
                )
            }
            outputSource.readByteArray()
        }
    }
}

context(scope: MediaSourceScope)
private fun MediaSource.ensureLocalized(): Path {
    if (this is LocalFileMediaSource) return this.path
    if (this is TempFileMediaSource) return this.path

    val source = tracked {
        TempFileMediaSource(
            kind = "ffmpeg-media",
            initialRawSource = openRawSource(),
        )
    }
    return source.path
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

private fun parseImageInfoFromFfmpegOutput(output: String): ImageInfo? {
    val videoLine = output.lineSequence().firstOrNull { "Video:" in it } ?: return null
    val resolution = resolutionRegex.find(videoLine) ?: return null
    val format = when {
        "png" in videoLine.lowercase() -> ImageFormat.PNG
        "gif" in videoLine.lowercase() -> ImageFormat.GIF
        "mjpeg" in videoLine.lowercase() || "jpeg" in videoLine.lowercase() || "jpg" in videoLine.lowercase() -> ImageFormat.JPEG
        "bmp" in videoLine.lowercase() -> ImageFormat.BMP
        "webp" in videoLine.lowercase() -> ImageFormat.WEBP
        "tiff" in videoLine.lowercase() -> ImageFormat.TIFF
        else -> return null
    }

    return ImageInfo(
        format = format,
        width = resolution.groupValues[1].toInt(),
        height = resolution.groupValues[2].toInt(),
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
