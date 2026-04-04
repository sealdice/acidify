package org.ntqqrev.acidify.milky

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.event.AcidifyEvent
import org.ntqqrev.acidify.milky.transform.transformAcidifyEvent
import org.ntqqrev.milky.Event

open class MilkyContext(
    val application: Application,
    val implName: String,
    val implVersion: String,
    val protocolOs: String,
    val httpAccessToken: String,
    val webhookEndpoints: List<WebhookEndpoint>,
    val reportSelfMessage: Boolean,
    val resolveUri: suspend (uri: String) -> ByteArray,
    val codec: Codec,
) : CoroutineScope by application {
    class WebhookEndpoint(
        val url: String,
        val accessToken: String,
    )

    val eventFlow = MutableSharedFlow<Event>(
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    internal fun pipeBotEventFlow(flow: Flow<AcidifyEvent>) = launch {
        val bot = application.dependencies.resolve<AbstractBot>()
        val logger = bot.createLogger(this@MilkyContext)
        flow.collect {
            try {
                val milkyEvent = transformAcidifyEvent(it) ?: return@collect
                eventFlow.emit(milkyEvent)
            } catch (e: Exception) {
                logger.w(e) { "处理事件 ${it::class.simpleName} 时发生错误" }
            }
        }
    }
}