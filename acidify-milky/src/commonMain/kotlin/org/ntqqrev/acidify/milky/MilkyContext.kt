package org.ntqqrev.acidify.milky

import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
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
        flow.mapNotNull { transformAcidifyEvent(it) }
            .collect {
                eventFlow.emit(it)
            }
    }
}