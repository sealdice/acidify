package com.github.ajalt.mordant.terminal

import com.github.ajalt.mordant.rendering.AnsiLevel

class Terminal(
    val ansiLevel: AnsiLevel = AnsiLevel.NONE,
) {
    fun println(message: Any?) {
        kotlin.io.println(message)
    }
}
