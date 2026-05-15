package org.ntqqrev.yogurt.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.milky.*
import org.ntqqrev.yogurt.YogurtApp.config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object FFmpegCodec : Codec {
    val ffmpegMutex = Mutex()

    override suspend fun getImageInfo(input: ByteArray): ImageInfo {
        return codecGetImageInfo(input)
    }

    override suspend fun audioToPcm(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        codecAudioToPcm(input)
    }

    override suspend fun silkEncode(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        codecSilkEncode(input)
    }

    override suspend fun calculatePcmDuration(
        input: ByteArray,
        bitDepth: Int,
        channelCount: Int,
        sampleRate: Int
    ): Duration {
        val bytesPerSample = bitDepth / 8.0
        if (bytesPerSample <= 0.0 || channelCount <= 0 || sampleRate <= 0) {
            return Duration.ZERO
        }
        val frameCount = input.size / (bytesPerSample * channelCount)
        return (frameCount / sampleRate).seconds
    }

    context(scope: MediaSourceScope)
    override suspend fun getVideoInfo(videoSource: MediaSource): VideoInfo = if (config.milky.ffmpegPath.isEmpty()) {
        ffmpegMutex.withLock {
            codecGetVideoInfo(videoSource)
        }
    } else {
        withContext(Dispatchers.IO) {
            val result = executeCommand(
                config.milky.ffmpegPath,
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
    override suspend fun getVideoFirstFrameJpg(videoSource: MediaSource): ByteArray =
        if (config.milky.ffmpegPath.isEmpty()) {
            ffmpegMutex.withLock {
                codecGetVideoFirstFrameJpg(videoSource)
            }
        } else {
            withContext(Dispatchers.IO) {
                val outputSource = tracked {
                    TempFileMediaSource(
                        kind = "ffmpeg-output",
                        ext = "jpg"
                    )
                }
                val result = executeCommand(
                    config.milky.ffmpegPath,
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
    if (this is LocalFileMediaSource) {
        return this.path
    }

    if (this is TempFileMediaSource) {
        return this.path
    }

    val source = tracked {
        TempFileMediaSource(
            kind = "ffmpeg-media",
            initialRawSource = openRawSource()
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
