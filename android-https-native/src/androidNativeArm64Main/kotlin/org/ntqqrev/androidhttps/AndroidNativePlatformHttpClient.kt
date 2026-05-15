package org.ntqqrev.androidhttps

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.coroutines.coroutineContext

fun createAndroidNativePlatformHttpClient(block: HttpClientConfig<*>.() -> Unit = {}): HttpClient = HttpClient(MockEngine) {
    block(this)
    engine {
        addHandler { request ->
            val requestBody = request.body.toByteArray()
            val response = executeBinaryRequest(
                method = request.method.value,
                url = request.url.toString(),
                headers = request.headers.entries().associate { (name, values) -> name to values.joinToString(",") },
                body = requestBody.takeIf { it.isNotEmpty() },
                contentType = request.headers[HttpHeaders.ContentType],
                followRedirects = false,
            )

            val responseHeaders = HeadersBuilder().apply {
                response.headers.forEach { (name, values) ->
                    values.forEach { value -> append(name, value) }
                }
            }.build()

            respond(
                content = ByteReadChannel(response.body),
                status = HttpStatusCode.fromValue(response.statusCode),
                headers = responseHeaders,
            )
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
private suspend fun OutgoingContent.toByteChannel(): ByteReadChannel = when (this) {
    is OutgoingContent.ByteArrayContent -> {
        val bytes = bytes()
        ByteReadChannel(bytes, 0, bytes.size)
    }
    is OutgoingContent.WriteChannelContent -> GlobalScope.writer(coroutineContext) {
        writeTo(channel)
    }.channel
    is OutgoingContent.ReadChannelContent -> readFrom()
    is OutgoingContent.NoContent -> ByteReadChannel.Empty
    is OutgoingContent.ContentWrapper -> delegate().toByteChannel()
    is OutgoingContent.ProtocolUpgrade -> error("Protocol upgrade is not supported on Android Native platform HTTP bridge")
}

private suspend fun OutgoingContent.toByteArray(): ByteArray {
    val channel = toByteChannel()
    val sink = Buffer()
    while (true) {
        val read = channel.readAvailable(1) { source: Buffer ->
            val bytes = source.readByteArray()
            sink.write(bytes)
            bytes.size
        }
        if (read <= 0) {
            break
        }
    }
    return sink.readByteArray()
}
