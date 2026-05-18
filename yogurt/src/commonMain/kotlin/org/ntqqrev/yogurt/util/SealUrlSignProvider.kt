package org.ntqqrev.yogurt.util

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.common.SignResult
import org.ntqqrev.acidify.exception.UrlSignException
import kotlin.time.Duration.Companion.seconds

/**
 * Seal-specific Lagrange V2 sign provider with JWT refresh and native Android HTTPS fallback.
 */
class SealUrlSignProvider(
    val url: String,
    val token: String,
    val uin: Long,
    val guid: String,
    val qua: String,
    val httpProxy: String? = null,
    jwtToken: String? = null,
    val launcherSignature: String? = null,
    val onJwtTokenUpdated: ((String) -> Unit)? = null,
) : SignProvider {
    private val signUrl = Url(url)
    private val jsonModule = Json {
        ignoreUnknownKeys = true
    }
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var jwtToken: String? = jwtToken?.takeUnless { it.isBlank() }
    private var refreshJob: Job? = null

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(jsonModule)
        }
        engine {
            if (!httpProxy.isNullOrEmpty()) {
                proxy = ProxyBuilder.http(httpProxy)
            }
        }
    }

    init {
        ensureRefreshStarted()
    }

    override suspend fun sign(cmd: String, seq: Int, src: ByteArray): SignResult {
        val currentJwtToken = jwtToken
        val currentLauncherSignature = launcherSignature
        val requestBody = jsonModule.encodeToString(
            SealUrlSignRequest(
                command = cmd,
                seq = seq,
                body = src.toHexString(),
                uin = uin,
                guid = guid,
                qua = qua,
            )
        )
        val requestHeaders = buildMap {
            when {
                !currentJwtToken.isNullOrEmpty() -> put(HttpHeaders.Authorization, "Bearer $currentJwtToken")
                !currentLauncherSignature.isNullOrEmpty() -> put("X-Launcher-Signature", currentLauncherSignature)
                else -> put(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        platformCurlTextRequestOrNull(
            method = "POST",
            url = URLBuilder(signUrl).apply { appendPathSegments("api", "sign", "sec-sign") }.buildString(),
            headers = requestHeaders,
            body = requestBody,
            contentType = ContentType.Application.Json.toString(),
            proxy = httpProxy,
        )?.let { response ->
            response.header("x-set-token")
                ?.takeUnless { it.isBlank() }
                ?.let(::updateJwtToken)
            return response.body.toSignResult()
        }

        val response = client.post {
            url {
                takeFrom(signUrl)
                appendPathSegments("api", "sign", "sec-sign")
            }
            requestHeaders.forEach { (name, value) -> header(name, value) }
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        response.headers["X-SET-TOKEN"]
            ?.takeUnless { it.isBlank() }
            ?.let(::updateJwtToken)
        val responseBody = response.body<SealUrlSignResponse>()
        return responseBody.toSignResult()
    }

    private fun String.toSignResult(): SignResult =
        jsonModule.decodeFromString<SealUrlSignResponse>(this).toSignResult()

    private fun SealUrlSignResponse.toSignResult(): SignResult {
        val responseValue = value
        if (code != 0 || responseValue == null) {
            throw UrlSignException(message ?: "", code)
        }
        return SignResult(
            sign = responseValue.sign.hexToByteArray(),
            token = responseValue.token.hexToByteArray(),
            extra = responseValue.extra.hexToByteArray(),
        )
    }

    private fun updateJwtToken(token: String) {
        jwtToken = token
        onJwtTokenUpdated?.invoke(token)
        ensureRefreshStarted()
    }

    private fun ensureRefreshStarted() {
        if (jwtToken.isNullOrEmpty() || refreshJob != null) {
            return
        }
        refreshJob = refreshScope.launch {
            while (isActive) {
                delay(300.seconds)
                runCatching {
                    refreshToken()
                }
            }
        }
    }

    private suspend fun refreshToken() {
        val currentJwtToken = jwtToken ?: return
        platformCurlTextRequestOrNull(
            method = "POST",
            url = URLBuilder(signUrl).apply { appendPathSegments("token", "refresh") }.buildString(),
            headers = mapOf(HttpHeaders.Authorization to "Bearer $currentJwtToken"),
            proxy = httpProxy,
        )?.let { response ->
            response.header("x-set-token")
                ?.takeUnless { it.isBlank() }
                ?.let(::updateJwtToken)
            return
        }

        val response = client.post {
            url {
                takeFrom(signUrl)
                appendPathSegments("token", "refresh")
            }
            header(HttpHeaders.Authorization, "Bearer $currentJwtToken")
        }
        response.headers["X-SET-TOKEN"]
            ?.takeUnless { it.isBlank() }
            ?.let(::updateJwtToken)
    }
}

@Serializable
private class SealUrlSignRequest(
    val command: String,
    val seq: Int,
    val body: String,
    val uin: Long,
    val guid: String,
    val qua: String,
)

@Serializable
private class SealUrlSignResponse(
    val code: Int = 0,
    val message: String? = null,
    val value: SealUrlSignValue? = null,
)

@Serializable
private class SealUrlSignValue(
    @SerialName("sec_sign") val sign: String,
    @SerialName("sec_token") val token: String,
    @SerialName("sec_extra") val extra: String,
)
