package org.ntqqrev.acidify.milky.event

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ntqqrev.acidify.milky.MilkyContext

context(ctx: MilkyContext)
fun Route.eventAuth() = install(createRouteScopedPlugin("EventAuth") {
    onCall { call ->
        if (
            call.request.headers["Authorization"] != "Bearer ${ctx.httpAccessToken}" &&
            call.request.queryParameters["access_token"] != ctx.httpAccessToken
        ) {
            call.respond(HttpStatusCode.Unauthorized)
            return@onCall
        }
    }
})