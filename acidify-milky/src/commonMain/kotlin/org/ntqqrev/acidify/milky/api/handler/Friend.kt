package org.ntqqrev.acidify.milky.api.handler

import org.ntqqrev.acidify.*
import org.ntqqrev.acidify.milky.api.define
import org.ntqqrev.acidify.milky.transform.toMilkyEntity
import org.ntqqrev.milky.*

val SendFriendNudge = ApiEndpoint.SendFriendNudge.define {
    bot.sendFriendNudge(it.userId, it.isSelf)
    SendFriendNudgeOutput()
}

val SendProfileLike = ApiEndpoint.SendProfileLike.define {
    bot.sendProfileLike(it.userId, it.count)
    SendProfileLikeOutput()
}

val DeleteFriend = ApiEndpoint.DeleteFriend.define {
    bot.deleteFriend(it.userId)
    DeleteFriendOutput()
}

val GetFriendRequests = ApiEndpoint.GetFriendRequests.define {
    val requests = bot.getFriendRequests(it.isFiltered, it.limit)
    GetFriendRequestsOutput(
        requests = requests.map { req -> req.toMilkyEntity() }
    )
}

val AcceptFriendRequest = ApiEndpoint.AcceptFriendRequest.define {
    bot.setFriendRequest(
        initiatorUid = it.initiatorUid,
        accept = true,
        isFiltered = it.isFiltered
    )
    AcceptFriendRequestOutput()
}

val RejectFriendRequest = ApiEndpoint.RejectFriendRequest.define {
    bot.setFriendRequest(
        initiatorUid = it.initiatorUid,
        accept = false,
        isFiltered = it.isFiltered
    )
    RejectFriendRequestOutput()
}
