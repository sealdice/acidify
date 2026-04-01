@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.ntqqrev.yogurt.util

import kotlinx.cinterop.toKString
import platform.posix.getenv

actual fun readEnvironmentVariableCompat(name: String): String? = getenv(name)?.toKString()
