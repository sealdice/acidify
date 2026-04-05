package org.ntqqrev.yogurt.util

import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.message.ImageFormat
import org.ntqqrev.acidify.milky.ImageInfo
import org.ntqqrev.acidify.milky.VideoInfo

internal actual suspend fun codecGetImageInfo(input: ByteArray): ImageInfo =
    org.ntqqrev.acidify.codec.getImageInfo(input).toAcidifyImageInfo()

internal actual suspend fun codecAudioToPcm(input: ByteArray): ByteArray =
    org.ntqqrev.acidify.codec.audioToPcm(input)

internal actual suspend fun codecSilkEncode(input: ByteArray): ByteArray =
    org.ntqqrev.acidify.codec.silkEncode(input)

internal actual suspend fun codecGetVideoInfo(videoSource: MediaSource): VideoInfo =
    org.ntqqrev.acidify.codec.getVideoInfo(videoSource.readByteArray()).toAcidifyVideoInfo()

internal actual suspend fun codecGetVideoFirstFrameJpg(videoSource: MediaSource): ByteArray =
    org.ntqqrev.acidify.codec.getVideoFirstFrameJpg(videoSource.readByteArray())

private fun org.ntqqrev.acidify.codec.ImageInfo.toAcidifyImageInfo() = ImageInfo(
    format = when (format) {
        org.ntqqrev.acidify.codec.ImageFormat.PNG -> ImageFormat.PNG
        org.ntqqrev.acidify.codec.ImageFormat.GIF -> ImageFormat.GIF
        org.ntqqrev.acidify.codec.ImageFormat.JPEG -> ImageFormat.JPEG
        org.ntqqrev.acidify.codec.ImageFormat.BMP -> ImageFormat.BMP
        org.ntqqrev.acidify.codec.ImageFormat.WEBP -> ImageFormat.WEBP
        org.ntqqrev.acidify.codec.ImageFormat.TIFF -> ImageFormat.TIFF
    },
    width = width,
    height = height,
)

private fun org.ntqqrev.acidify.codec.VideoInfo.toAcidifyVideoInfo() = VideoInfo(
    width = width,
    height = height,
    duration = duration,
)
