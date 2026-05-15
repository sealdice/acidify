package org.ntqqrev.yogurt.util

import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.message.ImageFormat
import org.ntqqrev.acidify.milky.ImageInfo
import org.ntqqrev.acidify.milky.VideoInfo
import org.ntqqrev.androidcodec.AndroidCodecImageFormat
import org.ntqqrev.androidcodec.AndroidNativeCodec

internal actual suspend fun codecGetImageInfo(input: ByteArray): ImageInfo =
    AndroidNativeCodec.getImageInfo(input).let { info ->
        ImageInfo(
            format = when (info.format) {
                AndroidCodecImageFormat.PNG -> ImageFormat.PNG
                AndroidCodecImageFormat.GIF -> ImageFormat.GIF
                AndroidCodecImageFormat.JPEG -> ImageFormat.JPEG
                AndroidCodecImageFormat.BMP -> ImageFormat.BMP
                AndroidCodecImageFormat.WEBP -> ImageFormat.WEBP
                AndroidCodecImageFormat.TIFF -> ImageFormat.TIFF
            },
            width = info.width,
            height = info.height,
        )
    }

internal actual suspend fun codecAudioToPcm(input: ByteArray): ByteArray = AndroidNativeCodec.audioToPcm(input)

internal actual suspend fun codecSilkEncode(input: ByteArray): ByteArray = AndroidNativeCodec.silkEncode(input)

internal actual suspend fun codecGetVideoInfo(videoSource: MediaSource): VideoInfo =
    AndroidNativeCodec.getVideoInfo(videoSource.readByteArray()).let { info ->
        VideoInfo(info.width, info.height, info.duration)
    }

internal actual suspend fun codecGetVideoFirstFrameJpg(videoSource: MediaSource): ByteArray =
    AndroidNativeCodec.getVideoFirstFrameJpg(videoSource.readByteArray())
