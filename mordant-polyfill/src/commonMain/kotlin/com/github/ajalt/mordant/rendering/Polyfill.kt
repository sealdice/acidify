package com.github.ajalt.mordant.rendering

enum class AnsiLevel {
    NONE,
    ANSI16,
    ANSI256,
    TRUECOLOR,
}

object TextColors {
    fun red(text: String): String = text
    fun green(text: String): String = text
    fun yellow(text: String): String = text
    fun cyan(text: String): String = text
    fun gray(text: String): String = text
    fun brightBlue(text: String): String = text
    fun brightGreen(text: String): String = text
    fun brightCyan(text: String): String = text
    fun brightYellow(text: String): String = text
    fun brightRed(text: String): String = text
}

object TextStyles {
    fun bold(text: String): String = text
}