package org.ntqqrev.acidify.common

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
import org.ntqqrev.acidify.exception.UrlSignException

/**
 * 通过 HTTP 接口进行签名的 [SignProvider] 实现，用于对接 Lagrange V2 Sign API。
 * 要对接普通的 Sign API，请使用 [UrlSignProvider]。
 * @param url 签名服务的 URL 地址
 * @param token 访问签名服务所需的 Token
 * @param uin 访问签名服务所用的 uin（QQ 号）
 * @param guid 当前登录设备的 GUID
 * @param qua 当前使用的 AppInfo 的 QUA 字符串，形如 `V1_LNX_NQ_3.2.**_*****_GW_B`
 * @param httpProxy 可选的 HTTP 代理地址，例如 `http://127.0.0.1:7890`
 * @param jwtToken 可选的 JWT Token，会优先于普通 token 使用
 * @param launcherSignature 可选的 APP_LAUNCHER_SIG，会在未提供 JWT Token 时使用
 * @param onJwtTokenUpdated 当服务端通过 `X-SET-TOKEN` 下发新 JWT Token 时的回调
 */
class LagrangeUrlSignProvider(
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

    override suspend fun sign(
        cmd: String,
        seq: Int,
        src: ByteArray
    ): SignResult {
        val currentJwtToken = jwtToken
        val currentLauncherSignature = launcherSignature
        val resp = client.post {
            url {
                takeFrom(signUrl)
                appendPathSegments("api", "sign", "sec-sign")
            }
            when {
                !currentJwtToken.isNullOrEmpty() -> header(HttpHeaders.Authorization, "Bearer $currentJwtToken")
                !currentLauncherSignature.isNullOrEmpty() -> header("X-Launcher-Signature", currentLauncherSignature)
                else -> header(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(ContentType.Application.Json)
            setBody(
                LagrangeUrlSignRequest(
                    command = cmd,
                    seq = seq,
                    body = src.toHexString(),
                    uin = uin,
                    guid = guid,
                    qua = qua,
                )
            )
        }
        resp.headers["X-SET-TOKEN"]
            ?.takeUnless { it.isBlank() }
            ?.let {
                updateJwtToken(it)
            }
        val respBody = resp.body<LagrangeUrlSignResponse>()
        if (respBody.code != 0 || respBody.value == null) {
            throw UrlSignException(respBody.message ?: "", respBody.code)
        }
        val value = respBody.value
        return SignResult(
            sign = value.sign.hexToByteArray(),
            token = value.token.hexToByteArray(),
            extra = value.extra.hexToByteArray(),
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
                delay(5 * 60 * 1000L)
                runCatching {
                    refreshToken()
                }
            }
        }
    }

    private suspend fun refreshToken() {
        val currentJwtToken = jwtToken ?: return
        val resp = client.post {
            url {
                takeFrom(signUrl)
                appendPathSegments("token", "refresh")
            }
            header(HttpHeaders.Authorization, "Bearer $currentJwtToken")
        }
        resp.headers["X-SET-TOKEN"]
            ?.takeUnless { it.isBlank() }
            ?.let {
                updateJwtToken(it)
            }
    }
}

@Serializable
private class LagrangeUrlSignRequest(
    val command: String,
    val seq: Int,
    val body: String,
    val uin: Long,
    val guid: String,
    val qua: String,
)

@Serializable
private class LagrangeUrlSignResponse(
    val code: Int = 0,
    val message: String? = null,
    val value: LagrangeUrlSignValue? = null,
)

@Serializable
private class LagrangeUrlSignValue(
    @SerialName("sec_sign") val sign: String,
    @SerialName("sec_token") val token: String,
    @SerialName("sec_extra") val extra: String,
)
