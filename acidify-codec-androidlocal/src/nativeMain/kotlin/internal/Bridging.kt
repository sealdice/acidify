@file:OptIn(ExperimentalForeignApi::class)

package org.ntqqrev.acidify.codec.internal

import kotlinx.cinterop.*
import kotlinx.io.Buffer
import kotlinx.io.readByteArray

internal typealias AudioFunction = (
    audioData: CValuesRef<UByteVarOf<UByte>>?,
    dataLen: Int,
    callback: CPointer<CFunction<(
        CPointer<out CPointed>?,
        CPointer<UByteVarOf<UByte>>?,
        Int
    ) -> Unit>>?,
    userData: CValuesRef<*>?
) -> Int

internal fun processAudio(input: ByteArray, func: AudioFunction): ByteArray = memScoped {
    val userData = Buffer()
    val userDataRef = StableRef.create(userData)
    val result = func.invoke(
        input.asUByteArray().toCValues(),
        input.size,
        staticCFunction { userData, p, len ->
            val buffer = userData!!.asStableRef<Buffer>().get()
            val byteArray = p!!.readBytes(len)
            buffer.write(byteArray)
        },
        userDataRef.asCPointer()
    )
    require(result == 0) { "audio processing failed with code $result" }
    userDataRef.dispose()
    return userData.readByteArray()
}