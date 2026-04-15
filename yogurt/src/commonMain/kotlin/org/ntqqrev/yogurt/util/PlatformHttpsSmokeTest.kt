package org.ntqqrev.yogurt.util

data class PlatformHttpsSmokeTestResponse(
    val statusCode: Int,
    val body: String,
)

expect fun platformHttpsSmokeTestOrNull(url: String): PlatformHttpsSmokeTestResponse?
