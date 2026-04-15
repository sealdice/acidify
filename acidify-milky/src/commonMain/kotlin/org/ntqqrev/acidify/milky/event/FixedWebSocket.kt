/**
 * Workaround for Ktor's WebSocket server on Android Native (androidNativeArm64).
 *
 * Root cause: ktor-io's androidNativeMain does not implement Charsets.ISO_8859_1
 * (throws IllegalStateException("not supported")). Ktor's WebSocket handshake
 * calls websocketServerAccept() which uses ISO_8859_1 to encode the accept key.
 *
 * Since the Sec-WebSocket-Key (base64) and the RFC 6455 GUID are pure ASCII,
 * UTF-8 encoding produces identical bytes and is a safe replacement.
 *
 * On platforms where ISO_8859_1 is available, this delegates to the standard
 * Ktor webSocket() implementation. No behavior change on non-Android-Native targets.
 */
package org.ntqqrev.acidify.milky.event

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val isIso88591Available: Boolean by lazy {
    try {
        Charsets.ISO_8859_1
        true
    } catch (_: IllegalStateException) {
        false
    }
}

/**
 * A WebSocket route builder that works on all platforms, including Android Native
 * where Charsets.ISO_8859_1 is not implemented.
 */
internal fun Route.fixedWebSocket(
    handler: suspend DefaultWebSocketServerSession.() -> Unit
) {
    if (isIso88591Available) {
        webSocket(handler = handler)
    } else {
        fixedWebSocketImpl(handler)
    }
}

// ---- Fixed implementation for platforms without ISO_8859_1 ----

/** RFC 6455 Section 4.2.2 magic GUID */
private const val WEBSOCKET_MAGIC = "258EAFA5-E914-47DA-95CA-5AB4C085B170"

private fun Route.fixedWebSocketImpl(
    handler: suspend DefaultWebSocketServerSession.() -> Unit
) {
    plugin(WebSockets) // early require

    header(HttpHeaders.Connection, "Upgrade") {
        header(HttpHeaders.Upgrade, "websocket") {
            handle {
                val webSockets = application.plugin(WebSockets)

                val upgrade = FixedWebSocketUpgrade(call) { rawSession ->
                    // Mirror Ktor's proceedWebSocket: wrap raw → default → server session
                    @OptIn(InternalAPI::class)
                    val session = DefaultWebSocketSession(
                        rawSession,
                        webSockets.pingIntervalMillis,
                        webSockets.timeoutMillis,
                        webSockets.channelsConfig,
                    ).apply {
                        val extensions = call.attributes[WebSockets.EXTENSIONS_KEY]
                        start(extensions)
                    }

                    val serverSession = FixedDefaultWebSocketServerSession(call, session)
                    try {
                        handler(serverSession)
                        session.close()
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (io: ChannelIOException) {
                        throw io
                    } catch (cause: Throwable) {
                        call.application.log.error("Websocket handler failed", cause)
                        throw cause
                    }

                    // Wait for session coroutines to finish (close sequence, etc.)
                    session.coroutineContext[Job]!!.join()
                }

                call.respond(upgrade)
            }
        }
    }
}

/**
 * A drop-in replacement for Ktor's WebSocketUpgrade that computes
 * Sec-WebSocket-Accept using UTF-8 instead of ISO_8859_1.
 */
private class FixedWebSocketUpgrade(
    private val call: ApplicationCall,
    private val handle: suspend (WebSocketSession) -> Unit
) : OutgoingContent.ProtocolUpgrade() {

    private val key = call.request.header(HttpHeaders.SecWebSocketKey)
    private val plugin = call.application.plugin(WebSockets)

    override val headers: Headers = Headers.build {
        append(HttpHeaders.Upgrade, "websocket")
        append(HttpHeaders.Connection, "Upgrade")
        if (key != null) {
            // Use encodeToByteArray() (UTF-8) instead of toByteArray(Charsets.ISO_8859_1).
            // The key is always base64 (ASCII) and the magic GUID is ASCII,
            // so UTF-8 and ISO_8859_1 produce identical byte sequences.
            @OptIn(ExperimentalEncodingApi::class)
            val acceptKey = Base64.encode(sha1("${key.trim()}$WEBSOCKET_MAGIC".encodeToByteArray()))
            append(HttpHeaders.SecWebSocketAccept, acceptKey)
        }
        // No extension negotiation in the fixed path
        call.attributes.put(WebSockets.EXTENSIONS_KEY, emptyList())
    }

    override suspend fun upgrade(
        input: ByteReadChannel,
        output: ByteWriteChannel,
        engineContext: CoroutineContext,
        userContext: CoroutineContext
    ): Job {
        val webSocket = RawWebSocket(
            input, output,
            plugin.maxFrameSize,
            plugin.masking,
            coroutineContext = engineContext + (coroutineContext[Job] ?: EmptyCoroutineContext),
            channelsConfig = plugin.channelsConfig
        )

        webSocket.launch(CoroutineName("raw-ws-handler")) {
            try {
                handle(webSocket)
                webSocket.flush()
            } catch (cause: Throwable) {
                webSocket.cancel("WebSocket is cancelled", cause)
            } finally {
                webSocket.cancel()
            }
        }

        return webSocket.coroutineContext[Job]!!
    }
}

// Delegation wrappers (same pattern as Ktor's internal DelegatedDefaultWebSocketServerSession)

private class FixedDefaultWebSocketServerSession(
    override val call: ApplicationCall,
    delegate: DefaultWebSocketSession
) : DefaultWebSocketServerSession, DefaultWebSocketSession by delegate
