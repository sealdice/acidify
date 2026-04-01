package com.github.ajalt.mordant.rendering

object TextStyles {
    fun bold(text: String): String {
        return if (text.contains("\u001B[")) {
            "\u001B[1m${text}\u001B[0m"
        } else {
            "\u001B[1m${text}\u001B[0m"
        }
    }
}
