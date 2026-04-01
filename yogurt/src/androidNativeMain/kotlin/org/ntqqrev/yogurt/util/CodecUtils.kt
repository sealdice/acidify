package org.ntqqrev.yogurt.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private fun org.ntqqrev.acidify.codec.ImageFormat.toCodecImageFormat() = when (this) {
    org.ntqqrev.acidify.codec.ImageFormat.PNG -> CodecImageFormat.PNG
    org.ntqqrev.acidify.codec.ImageFormat.GIF -> CodecImageFormat.GIF
    org.ntqqrev.acidify.codec.ImageFormat.JPEG -> CodecImageFormat.JPEG
    org.ntqqrev.acidify.codec.ImageFormat.BMP -> CodecImageFormat.BMP
    org.ntqqrev.acidify.codec.ImageFormat.WEBP -> CodecImageFormat.WEBP
    org.ntqqrev.acidify.codec.ImageFormat.TIFF -> CodecImageFormat.TIFF
}

actual object FFMpegCodec {
    private val ffmpegMutex = Mutex()

    actual suspend fun audioToPcm(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.audioToPcm(input)
    }

    actual suspend fun silkDecode(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.silkDecode(input)
    }

    actual suspend fun silkEncode(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.silkEncode(input)
    }

    actual suspend fun getVideoInfo(videoData: ByteArray): CodecVideoInfo = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.getVideoInfo(videoData).let {
            CodecVideoInfo(
                width = it.width,
                height = it.height,
                duration = it.duration,
            )
        }
    }

    actual suspend fun getVideoFirstFrameJpg(videoData: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.getVideoFirstFrameJpg(videoData)
    }
}

actual fun getCodecImageInfo(input: ByteArray): CodecImageInfo {
    val imageInfo = org.ntqqrev.acidify.codec.getImageInfo(input)
    return CodecImageInfo(
        format = imageInfo.format.toCodecImageFormat(),
        width = imageInfo.width,
        height = imageInfo.height,
    )
}

actual fun calculatePcmDurationCompat(input: ByteArray) = org.ntqqrev.acidify.codec.calculatePcmDuration(input)
