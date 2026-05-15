package org.ntqqrev.androidcodec

import kotlin.time.Duration

enum class AndroidCodecImageFormat {
    PNG,
    GIF,
    JPEG,
    BMP,
    WEBP,
    TIFF,
}

data class AndroidCodecImageInfo(
    val format: AndroidCodecImageFormat,
    val width: Int,
    val height: Int,
)

data class AndroidCodecVideoInfo(
    val width: Int,
    val height: Int,
    val duration: Duration,
)

object AndroidNativeCodec {
    suspend fun getImageInfo(input: ByteArray): AndroidCodecImageInfo =
        AndroidCodecImageInfo(AndroidCodecImageFormat.JPEG, 0, 0)

    suspend fun audioToPcm(input: ByteArray): ByteArray = ByteArray(0)

    suspend fun silkEncode(input: ByteArray): ByteArray = ByteArray(0)

    suspend fun getVideoInfo(input: ByteArray): AndroidCodecVideoInfo =
        AndroidCodecVideoInfo(0, 0, Duration.ZERO)

    suspend fun getVideoFirstFrameJpg(input: ByteArray): ByteArray = ByteArray(0)
}
