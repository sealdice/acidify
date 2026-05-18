package org.ntqqrev.yogurt

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.runBlocking
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.common.AppInfo
import org.ntqqrev.acidify.common.SessionStore
import org.ntqqrev.acidify.common.SignProvider
import org.ntqqrev.acidify.common.UrlSignProvider
import org.ntqqrev.acidify.offline
import org.ntqqrev.ktfs.withFs
import org.ntqqrev.yogurt.YogurtApp.config
import org.ntqqrev.yogurt.YogurtApp.t
import org.ntqqrev.yogurt.util.SealUrlSignProvider
import org.ntqqrev.yogurt.util.logHandler
import org.ntqqrev.yogurt.util.readEnvironmentVariable
import org.ntqqrev.yogurt.util.setEnvironmentVariable

suspend fun Application.initializeSeal(): Bot = withFs {
    val sessionStore: SessionStore = if (sessionStorePath.exists) {
        SessionStore.fromJson(sessionStorePath.readText())
    } else SessionStore.empty()

    var signProvider: SignProvider
    var appInfo: AppInfo

    fun readCustomAppInfo(): AppInfo {
        return if (customAppInfoPath.exists) {
            AppInfo.fromJson(customAppInfoPath.readText())
        } else {
            throw IllegalStateException("未在 $customAppInfoPath 下找到自定义 AppInfo 文件")
        }
    }

    fun readBundledAppInfo(): AppInfo {
        return bundledPCAppInfo["${config.protocol.os}/${config.protocol.version}"]
            ?: throw IllegalStateException("未找到匹配的内置 AppInfo，请检查配置的 OS 和 Version 是否正确")
    }

    if (config.protocol.pcLagrangeSignToken.isNotEmpty()) {
        require(config.protocol.uin != 0L) {
            "使用 Lagrange Sign API 时，请在配置文件中填写 uin 字段"
        }
        val launcherSignature = readEnvironmentVariable("APP_LAUNCHER_SIG")
        val jwtToken = readEnvironmentVariable("APP_JWT_TOKEN")
        appInfo = when (config.protocol.version) {
            "fetched" -> throw IllegalStateException("在使用 Lagrange Sign API 时，必须显式指定 AppInfo 版本或自行提供 AppInfo 文件，无法使用 fetched 版本")
            "custom" -> readCustomAppInfo()
            else -> readBundledAppInfo()
        }
        signProvider = SealUrlSignProvider(
            url = config.protocol.signApiUrl,
            token = config.protocol.pcLagrangeSignToken,
            uin = config.protocol.uin,
            guid = sessionStore.guid.toHexString(),
            qua = "V1_${
                when (config.protocol.os) {
                    "Windows" -> "WIN"
                    "Mac" -> "MAC"
                    "Linux" -> "LNX"
                    else -> throw IllegalStateException()
                }
            }_NQ_${appInfo.currentVersion.replace('-', '_')}_GW_B",
            jwtToken = jwtToken,
            launcherSignature = launcherSignature,
            onJwtTokenUpdated = { setEnvironmentVariable("APP_JWT_TOKEN", it) },
        )
    } else {
        signProvider = UrlSignProvider(config.protocol.signApiUrl)
        appInfo = when (config.protocol.version) {
            "fetched" -> signProvider.getAppInfo()
                ?: throw IllegalStateException("通过 Sign API 获取 AppInfo 失败，请检查地址是否正确并且支持获取 AppInfo 功能")

            "custom" -> readCustomAppInfo()
            else -> readBundledAppInfo()
        }
    }

    t.println("使用协议 ${appInfo.os} ${appInfo.currentVersion} (AppId: ${appInfo.subAppId})")
    val bot = Bot(
        appInfo = appInfo,
        sessionStore = sessionStore,
        signProvider = signProvider,
        scope = this@initializeSeal, // application is a CoroutineScope
        minLogLevel = config.logging.coreLogLevel,
        logHandler = YogurtApp.logHandler,
    )
    dependencies {
        provide<AbstractBot> { bot } cleanup { runBlocking { it.offline() } }
    }
    return bot
}