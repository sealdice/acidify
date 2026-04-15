@file:JvmName("Main")

package org.ntqqrev.yogurt

import com.github.ajalt.mordant.rendering.TextColors
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.ntqqrev.yogurt.YogurtApp.config
import org.ntqqrev.yogurt.YogurtApp.t
import org.ntqqrev.yogurt.util.createPlatformHttpClient
import org.ntqqrev.yogurt.util.isCausedByAddrInUse
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.milliseconds

fun main(args: Array<String>) {
    if (args.firstOrNull() == "test-https") {
        runHttpsTest(args.getOrNull(1) ?: "https://sealsignv3.sealdice.com/")
        return
    }

    val server = YogurtApp.createServer()
    try {
        server.start(wait = false)
        server.onSigint {
            server.stop(gracePeriodMillis = 2000L, timeoutMillis = 5000L)
        }
        runBlocking {
            delay(Long.MAX_VALUE.milliseconds)
        }
    } catch (e: Throwable) {
        if (e.isCausedByAddrInUse()) {
            t.println(
                TextColors.red(
                    """
                        无法启动服务器，可能是 ${config.milky.http.host}:${config.milky.http.port} 已被占用。
                        请检查是否有其他程序正在使用该地址，或者修改配置文件中的 host 和 port 后重试。
                    """.trimIndent()
                )
            )
        }
        throw e
    }
}

private fun runHttpsTest(url: String) = runBlocking {
    val httpClient = createPlatformHttpClient()
    try {
        val response = httpClient.get(url)
        val bodyPreview = response.bodyAsText().replace("\n", " ").replace("\r", " ").take(200)
        println("HTTPS test ok")
        println("URL: $url")
        println("Status: ${response.status}")
        println("Body: $bodyPreview")
    } catch (e: Throwable) {
        println("HTTPS test failed")
        println("URL: $url")
        println("Error: ${e::class.simpleName}: ${e.message}")
        halt(1)
    } finally {
        httpClient.close()
    }
}
