package org.ntqqrev.yogurt.util

import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.message.ImageFormat
import org.ntqqrev.acidify.milky.ImageInfo
import org.ntqqrev.acidify.milky.VideoInfo
import kotlin.time.Duration

internal actual suspend fun codecGetImageInfo(input: ByteArray): ImageInfo = ImageInfo(ImageFormat.JPEG, 0, 0)
internal actual suspend fun codecAudioToPcm(input: ByteArray): ByteArray = ByteArray(0)
internal actual suspend fun codecSilkEncode(input: ByteArray): ByteArray = ByteArray(0)
internal actual suspend fun codecGetVideoInfo(videoSource: MediaSource): VideoInfo = VideoInfo(0, 0, Duration.ZERO)
internal actual suspend fun codecGetVideoFirstFrameJpg(videoSource: MediaSource): ByteArray = ByteArray(0)
