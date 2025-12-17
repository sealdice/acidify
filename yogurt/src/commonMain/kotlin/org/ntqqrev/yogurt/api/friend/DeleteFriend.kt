package org.ntqqrev.yogurt.api.friend

import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*
import org.ntqqrev.acidify.Bot
import org.ntqqrev.milky.ApiEndpoint
import org.ntqqrev.milky.DeleteFriendOutput
import org.ntqqrev.yogurt.util.define

val DeleteFriend = ApiEndpoint.DeleteFriend.define {
    val bot = application.dependencies.resolve<Bot>()

    bot.deleteFriend(it.userId)

    DeleteFriendOutput()
}