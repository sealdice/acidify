package org.ntqqrev.yogurt.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ntqqrev.acidify.codec.VideoInfo

object FFMpegCodec {
    val ffmpegMutex = Mutex()

    suspend fun audioToPcm(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.audioToPcm(input)
    }

    suspend fun silkDecode(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.silkDecode(input)
    }

    suspend fun silkEncode(input: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.silkEncode(input)
    }

    suspend fun getVideoInfo(videoData: ByteArray): VideoInfo = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.getVideoInfo(videoData)
    }

    suspend fun getVideoFirstFrameJpg(videoData: ByteArray): ByteArray = ffmpegMutex.withLock {
        org.ntqqrev.acidify.codec.getVideoFirstFrameJpg(videoData)
    }
}