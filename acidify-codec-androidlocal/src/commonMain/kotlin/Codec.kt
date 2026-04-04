package org.ntqqrev.acidify.codec

import org.ntqqrev.acidify.codec.internal.commonImageInfoImpl
import org.ntqqrev.acidify.codec.internal.commonPcmDurationImpl
import kotlin.time.Duration

enum class ImageFormat {
    PNG, JPEG, GIF, BMP, WEBP, TIFF
}

data class ImageInfo(
    val format: ImageFormat,
    val width: Int,
    val height: Int,
)

data class VideoInfo(
    val width: Int,
    val height: Int,
    val duration: Duration,
)

fun getImageInfo(data: ByteArray): ImageInfo = commonImageInfoImpl(data)
    ?: throw IllegalArgumentException("Unsupported image format or corrupted data")

expect fun audioToPcm(input: ByteArray): ByteArray

expect fun silkDecode(input: ByteArray): ByteArray

expect fun silkEncode(input: ByteArray): ByteArray

fun calculatePcmDuration(
    input: ByteArray,
    bitDepth: Int = 16,
    channelCount: Int = 1,
    sampleRate: Int = 24000,
): Duration = commonPcmDurationImpl(input, bitDepth, channelCount, sampleRate)

expect fun getVideoInfo(videoData: ByteArray): VideoInfo

expect fun getVideoFirstFrameJpg(videoData: ByteArray): ByteArray