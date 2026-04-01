package org.ntqqrev.yogurt.script

import com.dokar.quickjs.QuickJs
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.*
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.milky.MilkyContext
import org.ntqqrev.milky.milkyJsonModule
import org.ntqqrev.yogurt.scriptsPath

suspend fun Application.loadScripts() {
    val bot = dependencies.resolve<AbstractBot>()
    val ctx = dependencies.resolve<MilkyContext>()
    val qjs = dependencies.resolve<QuickJs>()
    val logger = bot.createLogger("ScriptLoader")

    if (!SystemFileSystem.exists(scriptsPath)) {
        SystemFileSystem.createDirectories(scriptsPath)
    }

    val scripts = SystemFileSystem.list(scriptsPath)
        .filter { it.name.endsWith(".yogurtx.js") }
        .map {
            async {
                Script(
                    name = it.name,
                    content = SystemFileSystem.source(it).buffered().use { r ->
                        withContext(Dispatchers.IO) {
                            r.readString()
                        }
                    }
                )
            }
        }
        .awaitAll()
    if (scripts.isNotEmpty()) {
        scripts.forEach {
            qjs.evaluate<Any?>(
                code = it.content,
                filename = it.name,
                asModule = true,
            )
            logger.i { "脚本 ${it.name.removeSuffix(".yogurtx.js")} 加载完成" }
        }
        logger.i { "加载了 ${scripts.size} 个脚本" }
        ctx.eventFlow.collect {
            qjs.evaluate<Any?>("$internalEmitHandle(${milkyJsonModule.encodeToString(it)})")
        }
    }
}