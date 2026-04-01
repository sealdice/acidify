package com.github.ajalt.mordant.terminal

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.updateAnsiLevel

class Terminal(
    ansiLevel: AnsiLevel = AnsiLevel.ANSI256,
) {
    init {
        updateAnsiLevel(ansiLevel)
    }

    fun println(message: Any?) {
        kotlin.io.println(message)
    }
}
