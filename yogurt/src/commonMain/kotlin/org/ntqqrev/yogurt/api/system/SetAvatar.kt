package org.ntqqrev.yogurt.api.system

import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*
import org.ntqqrev.acidify.Bot
import org.ntqqrev.milky.ApiEndpoint
import org.ntqqrev.milky.SetAvatarOutput
import org.ntqqrev.yogurt.util.define
import org.ntqqrev.yogurt.util.resolveUri

val SetAvatar = ApiEndpoint.SetAvatar.define {
    val bot = application.dependencies.resolve<Bot>()

    bot.setAvatar(resolveUri(it.uri))

    SetAvatarOutput()
}