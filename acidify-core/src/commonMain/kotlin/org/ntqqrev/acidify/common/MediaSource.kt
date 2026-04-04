package org.ntqqrev.acidify.common

import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlin.js.JsExport

@JsExport
abstract class MediaSource {
    abstract val size: Long

    abstract fun openRawSource(): RawSource

    abstract fun dispose()

    open fun readByteArray(): ByteArray {
        return openRawSource().buffered().use { source ->
            source.readByteArray()
        }
    }

    companion object {
        @JsExport.Ignore
        fun ByteArray.toMediaSource(): MediaSource = ByteArrayMediaSource(this)
    }
}