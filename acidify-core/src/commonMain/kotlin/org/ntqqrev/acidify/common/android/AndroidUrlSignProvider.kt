@file:Suppress("duplicatedCode")

package org.ntqqrev.acidify.common.android

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ntqqrev.acidify.common.SignResult
import org.ntqqrev.acidify.exception.UrlSignException
import org.ntqqrev.acidify.internal.util.createPlatformHttpClient
import org.ntqqrev.acidify.internal.util.platformCurlTextRequestOrNull

/**
 * 通过 HTTP 接口进行签名的 [AndroidSignProvider] 实现
 * @param url 签名服务的 URL 地址
 * @param httpProxy 可选的 HTTP 代理地址，例如 `http://127.0.0.1:7890`
 */
class AndroidUrlSignProvider(val url: String, val httpProxy: String? = null) : AndroidSignProvider {
    val base = Url(url)
    private val jsonModule = Json { ignoreUnknownKeys = true }

    private val client = createPlatformHttpClient {
        install(ContentNegotiation) {
            json(jsonModule)
        }
        engine {
            if (!httpProxy.isNullOrEmpty()) {
                proxy = ProxyBuilder.http(httpProxy)
            }
        }
    }

    override suspend fun sign(
        uin: Long,
        cmd: String,
        buffer: ByteArray,
        guid: String,
        seq: Int,
        version: String,
        qua: String
    ): SignResult {
        platformCurlTextRequestOrNull(
            method = "POST",
            url = URLBuilder(base).apply { appendPathSegments("sign") }.buildString(),
            body = jsonModule.encodeToString(
                AndroidUrlSignRequest(
                    uin = uin,
                    cmd = cmd,
                    buffer = buffer.toHexString(),
                    guid = guid,
                    seq = seq,
                    version = version,
                    qua = qua,
                )
            ),
            contentType = ContentType.Application.Json.toString(),
            proxy = httpProxy,
        )?.let { resp ->
            ensureSuccess(resp.statusCode)
            val respBody = jsonModule.decodeFromString<AndroidUrlSignResponse<AndroidUrlSignValue>>(resp.body)
            if (respBody.code != 0 || respBody.data == null) {
                throw UrlSignException(respBody.msg, respBody.code)
            }
            return SignResult(
                sign = respBody.data.sign.hexToByteArray(),
                token = respBody.data.token.hexToByteArray(),
                extra = respBody.data.extra.hexToByteArray(),
            )
        }

        val resp = client.post {
            url {
                takeFrom(base)
                appendPathSegments("sign")
            }
            contentType(ContentType.Application.Json)
            setBody(
                AndroidUrlSignRequest(
                    uin = uin,
                    cmd = cmd,
                    buffer = buffer.toHexString(),
                    guid = guid,
                    seq = seq,
                    version = version,
                    qua = qua,
                )
            )
        }
        if (resp.status != HttpStatusCode.OK) {
            throw UrlSignException(resp.status.description, resp.status.value)
        }
        val respBody = resp.body<AndroidUrlSignResponse<AndroidUrlSignValue>>()
        if (respBody.code != 0 || respBody.data == null) {
            throw UrlSignException(respBody.msg, respBody.code)
        }
        return SignResult(
            sign = respBody.data.sign.hexToByteArray(),
            token = respBody.data.token.hexToByteArray(),
            extra = respBody.data.extra.hexToByteArray(),
        )
    }

    override suspend fun energy(
        uin: Long,
        data: String,
        guid: String,
        ver: String,
        version: String,
        qua: String
    ): ByteArray {
        platformCurlTextRequestOrNull(
            method = "POST",
            url = URLBuilder(base).apply { appendPathSegments("energy") }.buildString(),
            body = jsonModule.encodeToString(
                AndroidUrlEnergyRequest(
                    uin = uin,
                    data = data,
                    guid = guid,
                    ver = ver,
                    version = version,
                    qua = qua,
                )
            ),
            contentType = ContentType.Application.Json.toString(),
            proxy = httpProxy,
        )?.let { resp ->
            ensureSuccess(resp.statusCode)
            val respBody = jsonModule.decodeFromString<AndroidUrlSignResponse<String>>(resp.body)
            if (respBody.code != 0 || respBody.data == null) {
                throw UrlSignException(respBody.msg, respBody.code)
            }
            return respBody.data.hexToByteArray()
        }

        val resp = client.post {
            url {
                takeFrom(base)
                appendPathSegments("energy")
            }
            contentType(ContentType.Application.Json)
            setBody(
                AndroidUrlEnergyRequest(
                    uin = uin,
                    data = data,
                    guid = guid,
                    ver = ver,
                    version = version,
                    qua = qua,
                )
            )
        }
        if (resp.status != HttpStatusCode.OK) {
            throw UrlSignException(resp.status.description, resp.status.value)
        }
        val respBody = resp.body<AndroidUrlSignResponse<String>>()
        if (respBody.code != 0 || respBody.data == null) {
            throw UrlSignException(respBody.msg, respBody.code)
        }
        return respBody.data.hexToByteArray()
    }

    override suspend fun getDebugXwid(
        uin: Long,
        data: String,
        guid: String,
        version: String,
        qua: String
    ): ByteArray {
        platformCurlTextRequestOrNull(
            method = "POST",
            url = URLBuilder(base).apply { appendPathSegments("get_tlv553") }.buildString(),
            body = jsonModule.encodeToString(
                AndroidUrlDebugXwidRequest(
                    uin = uin,
                    data = data,
                    guid = guid,
                    version = version,
                    qua = qua,
                )
            ),
            contentType = ContentType.Application.Json.toString(),
            proxy = httpProxy,
        )?.let { resp ->
            ensureSuccess(resp.statusCode)
            val respBody = jsonModule.decodeFromString<AndroidUrlSignResponse<String>>(resp.body)
            if (respBody.code != 0 || respBody.data == null) {
                throw UrlSignException(respBody.msg, respBody.code)
            }
            return respBody.data.hexToByteArray()
        }

        val resp = client.post {
            url {
                takeFrom(base)
                appendPathSegments("get_tlv553")
            }
            contentType(ContentType.Application.Json)
            setBody(
                AndroidUrlDebugXwidRequest(
                    uin = uin,
                    data = data,
                    guid = guid,
                    version = version,
                    qua = qua,
                )
            )
        }
        if (resp.status != HttpStatusCode.OK) {
            throw UrlSignException(resp.status.description, resp.status.value)
        }
        val respBody = resp.body<AndroidUrlSignResponse<String>>()
        if (respBody.code != 0 || respBody.data == null) {
            throw UrlSignException(respBody.msg, respBody.code)
        }
        return respBody.data.hexToByteArray()
    }
}

@Serializable
private data class AndroidUrlSignRequest(
    val uin: Long,
    val cmd: String,
    val buffer: String,
    val guid: String,
    val seq: Int,
    val version: String,
    val qua: String
)

@Serializable
private data class AndroidUrlEnergyRequest(
    val uin: Long,
    val data: String,
    val guid: String,
    val ver: String,
    val version: String,
    val qua: String
)

@Serializable
private data class AndroidUrlDebugXwidRequest(
    val uin: Long,
    val data: String,
    val guid: String,
    val version: String,
    val qua: String
)

@Serializable
private data class AndroidUrlSignResponse<T>(
    val code: Int = 0,
    val msg: String = "",
    val data: T? = null,
)

private fun ensureSuccess(statusCode: Int) {
    if (statusCode != HttpStatusCode.OK.value) {
        throw UrlSignException(HttpStatusCode.fromValue(statusCode).description, statusCode)
    }
}

@Serializable
private data class AndroidUrlSignValue(
    val sign: String,
    val token: String,
    val extra: String
)
