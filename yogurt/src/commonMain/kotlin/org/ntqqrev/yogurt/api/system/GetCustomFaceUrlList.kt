package org.ntqqrev.yogurt.api.system

import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*
import org.ntqqrev.acidify.Bot
import org.ntqqrev.milky.ApiEndpoint
import org.ntqqrev.milky.GetCustomFaceUrlListOutput
import org.ntqqrev.yogurt.util.define

val GetCustomFaceUrlList = ApiEndpoint.GetCustomFaceUrlList.define {
    val bot = application.dependencies.resolve<Bot>()

    GetCustomFaceUrlListOutput(bot.getCustomFaceUrl())
}