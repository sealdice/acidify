package org.ntqqrev.acidify.milky.api

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.milky.ApiGeneralResponse

fun Route.apiLoginProtect() = install(createRouteScopedPlugin("ProtectNotLoggedIn") {
    onCall { call ->
        val bot = this@apiLoginProtect.application.dependencies.resolve<AbstractBot>()
        if (!bot.isLoggedIn) {
            call.respond(
                ApiGeneralResponse(
                    status = "failed",
                    retcode = -403,
                    message = "Bot is not logged in"
                )
            )
            return@onCall
        }
    }
})