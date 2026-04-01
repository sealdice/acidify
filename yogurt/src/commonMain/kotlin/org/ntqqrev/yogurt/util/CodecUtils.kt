package org.ntqqrev.yogurt.util

import kotlin.time.Duration

enum class CodecImageFormat {
    PNG,
    GIF,
    JPEG,
    BMP,
    WEBP,
    TIFF,
}

data class CodecImageInfo(
    val format: CodecImageFormat,
    val width: Int,
    val height: Int,
)

data class CodecVideoInfo(
    val width: Int,
    val height: Int,
    val duration: Duration,
)

expect object FFMpegCodec {
    suspend fun audioToPcm(input: ByteArray): ByteArray
    suspend fun silkDecode(input: ByteArray): ByteArray
    suspend fun silkEncode(input: ByteArray): ByteArray
    suspend fun getVideoInfo(videoData: ByteArray): CodecVideoInfo
    suspend fun getVideoFirstFrameJpg(videoData: ByteArray): ByteArray
}

expect fun getCodecImageInfo(input: ByteArray): CodecImageInfo

expect fun calculatePcmDurationCompat(input: ByteArray): Duration
