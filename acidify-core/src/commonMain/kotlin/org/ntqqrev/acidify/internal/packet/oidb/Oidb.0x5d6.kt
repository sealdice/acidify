package org.ntqqrev.acidify.internal.packet.oidb

import org.ntqqrev.acidify.internal.protobuf.*

internal object SetFriendPinReq : PbSchema() {
    val field1 = PbInt32[1]
    val info = Info[2]
    val field3 = PbInt32[3]

    internal object Info : PbSchema() {
        val friendUid = PbString[1]
        val field400 = Field400[400]

        internal object Field400 : PbSchema() {
            val field1 = PbInt32[1]
            val timestamp = PbBytes[2]
        }
    }
}

internal object SetGroupPinReq : PbSchema() {
    val field1 = PbInt32[1]
    val info = Info[2]
    val field3 = PbInt32[3]

    internal object Info : PbSchema() {
        val groupUin = PbInt64[2]
        val field400 = Field400[400]

        internal object Field400 : PbSchema() {
            val field1 = PbInt32[1]
            val timestamp = PbBytes[2]
        }
    }
}
