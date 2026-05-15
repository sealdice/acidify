package org.ntqqrev.yogurt.util

private val environmentOverrides = mutableMapOf<String, String?>()

fun readEnvironmentVariable(name: String): String? {
    if (environmentOverrides.containsKey(name)) {
        return environmentOverrides[name]
    }
    return readPlatformEnvironmentVariable(name)?.ifBlank { null }
}

fun setEnvironmentVariable(name: String, value: String?) {
    val normalizedValue = value?.ifBlank { null }
    environmentOverrides[name] = normalizedValue
    setPlatformEnvironmentVariable(name, normalizedValue)
}

internal expect fun readPlatformEnvironmentVariable(name: String): String?

internal expect fun setPlatformEnvironmentVariable(name: String, value: String?)
