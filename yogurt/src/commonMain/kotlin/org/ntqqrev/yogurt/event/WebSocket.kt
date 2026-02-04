package org.ntqqrev.yogurt.event

import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*
import io.ktor.server.routing.application
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.Bot
import org.ntqqrev.milky.Event

fun Route.configureMilkyEventWebSocket() = webSocket {
    val bot = application.dependencies.resolve<Bot>()
    val flow = application.dependencies.resolve<SharedFlow<Event>>()
    val logger = bot.createLogger("WebSocketModule")
    logger.i { "${call.request.local.remoteAddress} 通过 WebSocket 连接" }
    launch {
        flow.collect(::sendSerialized)
    }
    val reason = closeReason.await()
    if (reason?.code == CloseReason.Codes.NORMAL.code) {
        logger.i { "${call.request.local.remoteAddress} 断开 WebSocket 连接" }
    } else {
        logger.w { "${call.request.local.remoteAddress} 异常断开 WebSocket 连接 $reason" }
    }
}