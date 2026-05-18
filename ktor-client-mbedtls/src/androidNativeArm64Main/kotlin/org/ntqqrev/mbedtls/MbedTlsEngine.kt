package org.ntqqrev.mbedtls

import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.io.Buffer
import kotlinx.io.readByteArray

@OptIn(DelicateCoroutinesApi::class)
private suspend fun OutgoingContent.toByteChannel(): ByteReadChannel = when (this) {
    is OutgoingContent.ByteArrayContent -> {
        val bytes = bytes()
        ByteReadChannel(bytes, 0, bytes.size)
    }
    is OutgoingContent.WriteChannelContent -> GlobalScope.writer(currentCoroutineContext()) {
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

class MbedTlsEngine(override val config: HttpClientEngineConfig) : HttpClientEngineBase("ktor-mbedtls") {
    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val callContext = callContext()
        val requestBody = data.body.toByteArray()
        val response = executeBinaryRequest(
            method = data.method.value,
            url = data.url.toString(),
            headers = data.headers.entries().associate { (name, values) -> name to values.joinToString(",") },
            body = requestBody.takeIf { it.isNotEmpty() },
            contentType = data.headers[HttpHeaders.ContentType],
            followRedirects = false,
        )
        val responseHeaders = HeadersBuilder().apply {
            response.headers.forEach { (name, values) ->
                values.forEach { value -> append(name, value) }
            }
        }.build()
        return HttpResponseData(
            statusCode = HttpStatusCode.fromValue(response.statusCode),
            requestTime = GMTDate(),
            headers = responseHeaders,
            version = HttpProtocolVersion.HTTP_1_1,
            body = ByteReadChannel(response.body),
            callContext = callContext,
        )
    }
}