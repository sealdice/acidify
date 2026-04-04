package org.ntqqrev.yogurt.util

import org.ntqqrev.acidify.message.ImageFormat
import org.ntqqrev.acidify.milky.ImageInfo
import org.ntqqrev.acidify.milky.VideoInfo
import kotlin.time.Duration

internal actual suspend fun codecGetImageInfo(input: ByteArray): ImageInfo =
    ImageInfo(
        format = ImageFormat.JPEG,
        width = 0,
        height = 0,
    )

internal actual suspend fun codecAudioToPcm(input: ByteArray): ByteArray = ByteArray(0)

internal actual suspend fun codecSilkEncode(input: ByteArray): ByteArray = ByteArray(0)

internal actual suspend fun codecGetVideoInfo(videoData: ByteArray): VideoInfo =
    VideoInfo(
        width = 0,
        height = 0,
        duration = Duration.ZERO,
    )

internal actual suspend fun codecGetVideoFirstFrameJpg(videoData: ByteArray): ByteArray = ByteArray(0)
