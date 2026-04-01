package org.ntqqrev.yogurt.util

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

actual fun readByteArrayFromFilePath(path: String): ByteArray =
    SystemFileSystem.source(Path(path)).buffered().use {
        it.readByteArray()
    }
