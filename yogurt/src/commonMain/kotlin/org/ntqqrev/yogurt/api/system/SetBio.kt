package org.ntqqrev.yogurt.api.system

import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*
import org.ntqqrev.acidify.Bot
import org.ntqqrev.milky.ApiEndpoint
import org.ntqqrev.milky.SetBioOutput
import org.ntqqrev.yogurt.util.define

val SetBio = ApiEndpoint.SetBio.define {
    val bot = application.dependencies.resolve<Bot>()

    bot.setBio(it.newBio)

    SetBioOutput()
}