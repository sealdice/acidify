@file:OptIn(ExperimentalForeignApi::class)

package org.ntqqrev.acidify.codec

import kotlinx.cinterop.*
import liblagrangecodec.*
import org.ntqqrev.acidify.codec.internal.processAudio
import kotlin.time.Duration.Companion.seconds

actual fun audioToPcm(input: ByteArray) = processAudio(input, ::audio_to_pcm)

actual fun silkDecode(input: ByteArray) = processAudio(input, ::silk_decode)

actual fun silkEncode(input: ByteArray) = processAudio(input, ::silk_encode)

actual fun getVideoInfo(videoData: ByteArray): VideoInfo = memScoped {
    val infoStruct = alloc<liblagrangecodec.VideoInfo>()
    val result = video_get_size(
        video_data = videoData.asUByteArray().toCValues(),
        data_len = videoData.size,
        info = infoStruct.ptr
    )
    require(result == 0) { "videoGetSize failed with code $result" }
    return VideoInfo(
        width = infoStruct.width,
        height = infoStruct.height,
        duration = infoStruct.duration.seconds
    )
}

actual fun getVideoFirstFrameJpg(videoData: ByteArray): ByteArray = memScoped {
    val outputDataPtr = alloc<CPointerVar<UByteVar>>()
    val outputLenPtr = alloc<IntVar>()
    val result = video_first_frame(
        video_data = videoData.asUByteArray().toCValues(),
        data_len = videoData.size,
        out = outputDataPtr.ptr,
        out_len = outputLenPtr.ptr
    )
    require(result == 0) { "videoFirstFrame failed with code $result" }
    val outputLen = outputLenPtr.value
    val outputData = outputDataPtr.value!!
    val byteArray = ByteArray(outputLen)
    for (i in 0 until outputLen) {
        byteArray[i] = outputData[i].toByte()
    }
    return byteArray
}