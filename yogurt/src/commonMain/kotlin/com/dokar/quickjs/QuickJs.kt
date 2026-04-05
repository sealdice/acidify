package com.dokar.quickjs

import com.dokar.quickjs.binding.ObjectBindingScope
import kotlinx.coroutines.CoroutineDispatcher

class QuickJs {
    companion object {
        suspend fun create(jobDispatcher: CoroutineDispatcher? = null): QuickJs = QuickJs()
    }

    fun close() = Unit

    @Suppress("UNUSED_PARAMETER")
    inline fun <reified T> evaluate(
        code: String,
        filename: String? = null,
        asModule: Boolean = false,
    ): T? = null
}
