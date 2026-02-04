package org.ntqqrev.yogurt.event

import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.Bot
import org.ntqqrev.milky.Event
import org.ntqqrev.milky.milkyJsonModule

fun Route.configureMilkyEventSse() = sse {
    val bot = application.dependencies.resolve<Bot>()
    val flow = application.dependencies.resolve<SharedFlow<Event>>()
    val logger = bot.createLogger("SseModule")
    logger.i { "${call.request.local.remoteAddress} 通过 SSE 连接" }
    launch {
        flow.collect {
            send(
                data = milkyJsonModule.encodeToString(it),
                event = "milky_event"
            )
        }
    }
}