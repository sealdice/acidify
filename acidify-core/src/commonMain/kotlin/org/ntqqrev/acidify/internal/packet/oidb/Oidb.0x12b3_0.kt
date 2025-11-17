package org.ntqqrev.acidify.internal.packet.oidb

import org.ntqqrev.acidify.internal.protobuf.*

internal object FetchPinsResp : PbSchema() {
    val friends = PbRepeated[Friend[1]]
    val groups = PbRepeated[Group[3]]

    internal object Friend : PbSchema() {
        val uid = PbString[1]
    }

    internal object Group : PbSchema() {
        val uin = PbInt64[1]
    }
}
