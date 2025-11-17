package org.ntqqrev.acidify.internal.packet.misc

import org.ntqqrev.acidify.internal.protobuf.*

internal object FaceRoamRequest : PbSchema() {
    val comm = PlatInfo[1]
    val selfUin = PbInt64[2]
    val subCmd = PbInt32[3]
    val field6 = PbInt32[6]

    internal object PlatInfo : PbSchema() {
        val imPlat = PbInt32[1]
        val osVersion = PbString[2]
        val qVersion = PbString[3]
    }
}

internal object FaceRoamResponse : PbSchema() {
    val retCode = PbInt32[1]
    val errMsg = PbString[2]
    val subCmd = PbInt32[3]
    val userInfo = UserInfo[4]

    internal object UserInfo : PbSchema() {
        val fileName = PbRepeatedString[1]
        val deleteFile = PbRepeatedString[2]
        val bid = PbString[3]
        val maxRoamSize = PbInt32[4]
        val emojiType = PbRepeatedInt32[5]
    }
}
