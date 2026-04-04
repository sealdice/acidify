package org.ntqqrev.yogurt.util

import org.ntqqrev.acidify.logging.LogHandler
import org.ntqqrev.yogurt.YogurtApp
import org.ntqqrev.yogurt.YogurtApp.t

actual val YogurtApp.logHandler: LogHandler by lazy {
    LogHandler { level, tag, message, throwable ->
        t.println(
            formatColoredLog(
                level,
                tag,
                message,
                throwable?.stackTraceToString(),
            )
        )
    }
}
