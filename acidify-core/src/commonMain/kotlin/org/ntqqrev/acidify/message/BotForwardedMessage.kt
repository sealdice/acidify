package org.ntqqrev.acidify.message

import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.internal.packet.message.CommonMessage
import org.ntqqrev.acidify.internal.protobuf.PbObject
import org.ntqqrev.acidify.message.BotIncomingMessage.Companion.buildSegments
import kotlin.js.JsExport

/**
 * 合并转发消息中的单条消息
 * @property senderName 发送者名称
 * @property avatarUrl 发送者头像 URL
 * @property timestamp 消息发送的 Unix 时间戳（秒）
 * @property segments 消息内容
 */
@JsExport
class BotForwardedMessage internal constructor(
    val senderName: String,
    val avatarUrl: String,
    val timestamp: Long,
) {
    lateinit var segments: List<BotIncomingSegment>
        internal set

    companion object {
        internal fun Bot.parseForwardedMessage(raw: PbObject<CommonMessage>): BotForwardedMessage? {
            val routingHead = raw.get { routingHead }
            val contentHead = raw.get { contentHead }
            val senderName = routingHead.get { commonC2C }.get { name }.takeIf { it.isNotEmpty() }
                ?: routingHead.get { group }.get { groupCard }.takeIf { it.isNotEmpty() }
                ?: "QQ用户"
            val avatarUrl = contentHead.get { forwardExt }.get { avatar }
            val message = BotForwardedMessage(
                senderName = senderName,
                avatarUrl = avatarUrl,
                timestamp = contentHead.get { time }
            )
            val segments = buildSegments(
                elems = raw.get { messageBody }.get { richText }.get { this.elems },
                scene = MessageScene.FRIEND
            )
            if (segments.isEmpty()) {
                return null
            }
            message.segments = segments
            return message
        }
    }
}

