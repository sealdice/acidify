@file:OptIn(ExperimentalForeignApi::class)

package org.ntqqrev.yogurt.util

import kotlinx.cinterop.*
import kotlinx.io.IOException
import kotlinx.io.files.FileNotFoundException
import platform.windows.*

actual fun readByteArrayFromFilePath(path: String): ByteArray = memScoped {
    val handle = CreateFileW(
        path.removePrefix("/"),
        GENERIC_READ,
        FILE_SHARE_READ.toUInt(),
        null,
        OPEN_EXISTING.toUInt(),
        FILE_ATTRIBUTE_NORMAL.toUInt(),
        null
    )

    if (handle == INVALID_HANDLE_VALUE) {
        val err = GetLastError()
        when (err.toInt()) {
            ERROR_FILE_NOT_FOUND -> throw FileNotFoundException("File does not exist: $path")
            else -> throw IOException("Failed to open $path (error=$err)")
        }
    }

    try {
        val sizeStruct = alloc<LARGE_INTEGER>()
        if (GetFileSizeEx(handle, sizeStruct.ptr) == 0) {
            val err = GetLastError()
            throw IOException("Failed to get file size for $path (error=$err)")
        }

        val fileSize = sizeStruct.QuadPart
        if (fileSize < 0) {
            throw IOException("Invalid size ($fileSize)")
        }
        if (fileSize == 0L) {
            return ByteArray(0)
        }
        if (fileSize > Int.MAX_VALUE.toLong()) {
            throw IOException("File is too large to read into memory ($fileSize bytes): $path")
        }
        val buffer = ByteArray(fileSize.toInt())
        buffer.usePinned { pinned ->
            val bytesRead = alloc<DWORDVar>()
            val ok = ReadFile(
                handle,
                pinned.addressOf(0),
                fileSize.toUInt(),
                bytesRead.ptr,
                null
            )

            if (ok == 0 || bytesRead.value.toLong() != fileSize) {
                val err = GetLastError()
                throw IOException("Incomplete read of file $path (error=$err, read=${bytesRead.value}, expected=$fileSize)")
            }
        }

        return buffer
    } finally {
        CloseHandle(handle)
    }
}
