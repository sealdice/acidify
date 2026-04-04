package org.ntqqrev.acidify.milky.api

import io.ktor.server.application.*
import org.ntqqrev.acidify.AbstractBot
import org.ntqqrev.acidify.milky.Codec
import org.ntqqrev.acidify.milky.MilkyContext

class MilkyApiContext(
    val bot: AbstractBot,
    application: Application,
    implName: String,
    implVersion: String,
    protocolOs: String,
    httpAccessToken: String,
    webhookEndpoints: List<WebhookEndpoint>,
    reportSelfMessage: Boolean,
    resolveUri: suspend (uri: String) -> ByteArray,
    codec: Codec,
) : MilkyContext(
    application,
    implName,
    implVersion,
    protocolOs,
    httpAccessToken,
    webhookEndpoints,
    reportSelfMessage,
    resolveUri,
    codec
) {
    constructor(bot: AbstractBot, ctx: MilkyContext) : this(
        bot,
        ctx.application,
        ctx.implName,
        ctx.implVersion,
        ctx.protocolOs,
        ctx.httpAccessToken,
        ctx.webhookEndpoints,
        ctx.reportSelfMessage,
        ctx.resolveUri,
        ctx.codec,
    )
}