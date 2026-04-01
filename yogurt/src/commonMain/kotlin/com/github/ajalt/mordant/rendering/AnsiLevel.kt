package com.github.ajalt.mordant.rendering

import kotlinx.serialization.Serializable

@Serializable
enum class AnsiLevel {
    NONE,
    ANSI16,
    ANSI256,
    TRUECOLOR,
}
