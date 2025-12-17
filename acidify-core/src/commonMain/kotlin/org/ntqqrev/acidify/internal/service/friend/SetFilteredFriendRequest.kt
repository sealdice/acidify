package org.ntqqrev.acidify.internal.service.friend

import org.ntqqrev.acidify.internal.LagrangeClient
import org.ntqqrev.acidify.internal.packet.oidb.SetFilteredFriendRequestReq
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.internal.service.NoOutputOidbService

internal object SetFilteredFriendRequest : NoOutputOidbService<String>(0xd72, 0) {
    override fun buildOidb(client: LagrangeClient, payload: String): ByteArray =
        SetFilteredFriendRequestReq {
            it[selfUid] = client.sessionStore.uid
            it[requestUid] = payload
        }.toByteArray()
}
