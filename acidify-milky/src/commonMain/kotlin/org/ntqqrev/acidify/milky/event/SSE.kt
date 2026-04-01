package org.ntqqrev.acidify.milky.event

import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.milky.MilkyContext
import org.ntqqrev.milky.milkyJsonModule

context(ctx: MilkyContext)
fun Route.eventSse() = sse {
    val bot = application.dependencies.resolve<AbstractBot>()
    val logger = bot.createLogger("SseModule")
    logger.i { "${call.request.local.remoteAddress} 通过 SSE 连接" }
    launch {
        ctx.eventFlow.collect {
            send(
                data = milkyJsonModule.encodeToString(it),
                event = "milky_event"
            )
        }
    }
}