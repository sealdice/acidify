package org.ntqqrev.yogurt.util

internal actual fun readPlatformEnvironmentVariable(name: String): String? = System.getenv(name)?.ifBlank { null }

internal actual fun setPlatformEnvironmentVariable(name: String, value: String?) = Unit
