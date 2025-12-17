package org.ntqqrev.yogurt.api.system

import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*
import org.ntqqrev.acidify.Bot
import org.ntqqrev.milky.ApiEndpoint
import org.ntqqrev.milky.SetNicknameOutput
import org.ntqqrev.yogurt.util.define

val SetNickname = ApiEndpoint.SetNickname.define {
    val bot = application.dependencies.resolve<Bot>()

    bot.setNickname(it.newCard)

    SetNicknameOutput()
}