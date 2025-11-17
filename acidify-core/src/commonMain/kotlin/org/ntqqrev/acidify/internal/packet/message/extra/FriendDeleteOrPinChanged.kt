package org.ntqqrev.acidify.internal.packet.message.extra

import org.ntqqrev.acidify.internal.protobuf.*

internal object FriendDeleteOrPinChanged : PbSchema() {
    val body = Body[1]

    internal object Body : PbSchema() {
        val type = PbInt32[2]
        val pinChanged = PbOptional[PinChanged[20]]
    }

    internal object PinChanged : PbSchema() {
        val body = PinChangedBody[1]

        internal object PinChangedBody : PbSchema() {
            val uid = PbString[1]
            val groupUin = PbOptional[PbInt64[2]]
            val info = PinChangedInfo[400]
        }
    }

    internal object PinChangedInfo : PbSchema() {
        val timestamp = PbBytes[2]
    }
}