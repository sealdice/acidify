package org.ntqqrev.yogurt.util

import org.ntqqrev.acidify.common.MediaSource
import org.ntqqrev.acidify.milky.ImageInfo
import org.ntqqrev.acidify.milky.VideoInfo

internal expect suspend fun codecGetImageInfo(input: ByteArray): ImageInfo
internal expect suspend fun codecAudioToPcm(input: ByteArray): ByteArray
internal expect suspend fun codecSilkEncode(input: ByteArray): ByteArray
internal expect suspend fun codecGetVideoInfo(videoSource: MediaSource): VideoInfo
internal expect suspend fun codecGetVideoFirstFrameJpg(videoSource: MediaSource): ByteArray
