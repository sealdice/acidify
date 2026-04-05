@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.ntqqrev.yogurt.util

import kotlinx.cinterop.toKString
import platform.posix.getenv
import platform.windows.SetEnvironmentVariableW

internal actual fun readPlatformEnvironmentVariable(name: String): String? = getenv(name)?.toKString()?.ifBlank { null }

internal actual fun setPlatformEnvironmentVariable(name: String, value: String?) {
    SetEnvironmentVariableW(name, value)
}
