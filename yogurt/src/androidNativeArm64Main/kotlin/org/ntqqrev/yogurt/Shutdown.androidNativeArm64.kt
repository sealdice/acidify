package org.ntqqrev.yogurt

import io.ktor.server.engine.EmbeddedServer
import platform.posix._exit

actual fun EmbeddedServer<*, *>.onSigint(hook: () -> Unit) = Unit

actual fun halt(status: Int) {
    _exit(status)
}
