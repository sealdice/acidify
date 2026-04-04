package org.ntqqrev.acidify.milky.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ntqqrev.acidify.milky.MilkyContext

context(ctx: MilkyContext)
fun Route.apiAuth() = install(createRouteScopedPlugin("ApiAuth") {
    onCall { call ->
        if (call.request.headers["Authorization"] != "Bearer ${ctx.httpAccessToken}") {
            call.respond(HttpStatusCode.Unauthorized)
            return@onCall
        }
    }
})