package com.github.ajalt.mordant.rendering

private object AnsiState {
    var level: AnsiLevel = AnsiLevel.ANSI256
}

internal fun updateAnsiLevel(level: AnsiLevel) {
    AnsiState.level = level
}

private fun style(code: String, text: String): String {
    if (AnsiState.level == AnsiLevel.NONE) {
        return text
    }
    return "\u001B[${code}m${text}\u001B[0m"
}

object TextColors {
    fun gray(text: String) = style("90", text)
    fun brightBlue(text: String) = style("94", text)
    fun green(text: String) = style("32", text)
    fun brightYellow(text: String) = style("93", text)
    fun brightRed(text: String) = style("91", text)
    fun cyan(text: String) = style("36", text)
    fun yellow(text: String) = style("33", text)
    fun brightGreen(text: String) = style("92", text)
    fun brightCyan(text: String) = style("96", text)
    fun red(text: String) = style("31", text)
}
