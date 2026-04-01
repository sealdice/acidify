package org.ntqqrev.yogurt.util

actual fun readEnvironmentVariableCompat(name: String): String? = System.getenv(name)
