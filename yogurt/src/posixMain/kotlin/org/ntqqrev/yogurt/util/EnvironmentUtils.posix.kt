@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.ntqqrev.yogurt.util

import kotlinx.cinterop.toKString
import platform.posix.getenv
import platform.posix.setenv
import platform.posix.unsetenv

internal actual fun readPlatformEnvironmentVariable(name: String): String? = getenv(name)?.toKString()?.ifBlank { null }

internal actual fun setPlatformEnvironmentVariable(name: String, value: String?) {
    if (value == null) {
        unsetenv(name)
    } else {
        setenv(name, value, 1)
    }
}
