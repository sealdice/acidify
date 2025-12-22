package org.ntqqrev.acidify.message.internal

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.ntqqrev.acidify.Bot
import org.ntqqrev.acidify.internal.packet.message.CommonMessage
import org.ntqqrev.acidify.internal.packet.message.Elem
import org.ntqqrev.acidify.internal.packet.message.PushMsgType
import org.ntqqrev.acidify.internal.packet.message.extra.PrivateFileExtra
import org.ntqqrev.acidify.internal.packet.misc.GroupEssenceMsgItem
import org.ntqqrev.acidify.internal.protobuf.PbObject
import org.ntqqrev.acidify.internal.protobuf.PbOptional
import org.ntqqrev.acidify.internal.protobuf.PbSchema
import org.ntqqrev.acidify.internal.protobuf.invoke
import org.ntqqrev.acidify.message.*
import org.ntqqrev.acidify.message.BotIncomingMessage.ExtraInfo

internal class MessageParsingContext(
    val scene: MessageScene,
    val elems: List<PbObject<Elem>>,
    val bot: Bot,
) {
    var currentIndex = 0

    val remainingCount: Int
        get() = elems.size - currentIndex

    fun hasNext(): Boolean = currentIndex < elems.size

    fun peek(): PbObject<Elem> = elems[currentIndex]

    fun <T : PbSchema> tryPeekType(type: PbOptional<PbObject<T>>): PbObject<T>? = peek()[type]

    inline fun <T : PbSchema> tryPeekType(typeProvider: Elem.() -> PbOptional<PbObject<T>>) =
        tryPeekType(Elem.typeProvider())

    fun skip(count: Int = 1) {
        if (currentIndex + count > elems.size) {
            throw IndexOutOfBoundsException("Cannot skip $count elements from index $currentIndex, size is ${elems.size}")
        }
        currentIndex += count
    }

    fun consume() = skip(1)

    companion object {
        internal val factories = listOf(
            IncomingSegmentFactory.Text,
            IncomingSegmentFactory.Mention,
            IncomingSegmentFactory.Face,
            IncomingSegmentFactory.Reply,
            IncomingSegmentFactory.Image,
            IncomingSegmentFactory.Record,
            IncomingSegmentFactory.Video,
            IncomingSegmentFactory.File,
            IncomingSegmentFactory.Forward,
            IncomingSegmentFactory.MarketFace,
            IncomingSegmentFactory.LightApp,
        )

        internal fun Bot.parseMessage(raw: PbObject<CommonMessage>): BotIncomingMessage? {
            val contentHead = raw.get { contentHead }
            val pushMsgType = PushMsgType.from(contentHead.get { type }) ?: return null
            val draftMsg = buildDraftMessage(raw, pushMsgType) ?: return null
            val segments = if (pushMsgType != PushMsgType.FriendFileMessage) {
                buildSegments(raw.get { messageBody }.get { richText }.get { elems }, draftMsg.scene) {
                    draftMsg.extraInfo = it
                }
            } else {
                val notOnlineFile = PrivateFileExtra(raw.get { messageBody }.get { msgContent })
                    .get { notOnlineFile }
                listOf(
                    BotIncomingSegment.File(
                        fileId = notOnlineFile.get { fileUuid },
                        fileName = notOnlineFile.get { fileName },
                        fileSize = notOnlineFile.get { fileSize },
                        fileHash = notOnlineFile.get { fileIdCrcMedia }
                    )
                )
            }

            if (segments.isEmpty()) {
                return null
            }

            draftMsg.segments = segments
            return draftMsg
        }

        private fun Bot.buildDraftMessage(raw: PbObject<CommonMessage>, pushMsgType: PushMsgType): BotIncomingMessage? {
            val routingHead = raw.get { routingHead }
            val contentHead = raw.get { contentHead }

            return when (pushMsgType) {
                PushMsgType.FriendMessage,
                PushMsgType.FriendRecordMessage,
                PushMsgType.FriendFileMessage -> {
                    val isSelfSend = routingHead.get { fromUin } == this.uin
                    BotIncomingMessage(
                        scene = MessageScene.FRIEND,
                        peerUin = if (isSelfSend) routingHead.get { toUin } else routingHead.get { fromUin },
                        peerUid = if (isSelfSend) routingHead.get { toUid } else routingHead.get { fromUid },
                        sequence = contentHead.get { clientSequence },
                        timestamp = contentHead.get { time },
                        senderUin = routingHead.get { fromUin },
                        senderUid = routingHead.get { fromUid },
                        clientSequence = contentHead.get { sequence }, // weird
                        random = contentHead.get { random },
                        messageUid = contentHead.get { msgUid },
                        raw = raw,
                    )
                }

                PushMsgType.GroupMessage -> BotIncomingMessage(
                    scene = MessageScene.GROUP,
                    peerUin = routingHead.get { group }.get { groupCode },
                    peerUid = routingHead.get { toUid },
                    sequence = contentHead.get { sequence },
                    timestamp = contentHead.get { time },
                    senderUin = routingHead.get { fromUin },
                    senderUid = routingHead.get { fromUid },
                    clientSequence = contentHead.get { clientSequence },
                    random = contentHead.get { random },
                    messageUid = contentHead.get { msgUid },
                    raw = raw,
                )

                else -> null
            }
        }

        internal fun Bot.buildSegments(
            elems: List<PbObject<Elem>>,
            scene: MessageScene,
            onExtraInfo: ((ExtraInfo) -> Unit)? = null
        ): List<BotIncomingSegment> {
            val segments = mutableListOf<BotIncomingSegment>()
            val ctx = MessageParsingContext(scene, elems, this)
            while (ctx.hasNext()) {
                ctx.tryPeekType { this.extraInfo }?.let {
                    onExtraInfo?.invoke(
                        ExtraInfo(
                            nick = it.get { nick },
                            groupCard = it.get { groupCard },
                            specialTitle = it.get { senderTitle },
                        )
                    )
                    ctx.consume()
                    continue
                }

                var matched = false
                for (factory in factories) {
                    val segment = factory.tryParse(ctx) ?: continue
                    segments += segment
                    matched = true
                    break
                }
                if (!matched) {
                    ctx.skip()
                }
            }

            return segments
        }

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

        internal fun GroupEssenceMsgItem.toBotEssenceMessage(groupUin: Long) = BotEssenceMessage(
            groupUin = groupUin,
            messageSeq = this.msgSeq,
            messageTime = this.senderTime,
            senderUin = this.senderUin.toLongOrNull() ?: 0L,
            senderName = this.senderNick,
            operatorUin = this.addDigestUin.toLongOrNull() ?: 0L,
            operatorName = this.addDigestNick,
            operationTime = this.addDigestTime,
            segments = this.msgContent.mapNotNull { element ->
                val msgType = element["msg_type"]?.jsonPrimitive?.int ?: return@mapNotNull null
                when (msgType) {
                    1 -> BotEssenceSegment.Text(element["text"]?.jsonPrimitive?.content ?: "")
                    2 -> BotEssenceSegment.Face(element["face_index"]?.jsonPrimitive?.int ?: 0)
                    3 -> BotEssenceSegment.Image(element["image_url"]?.jsonPrimitive?.content ?: "")
                    4 -> BotEssenceSegment.Video(element["file_thumbnail_url"]?.jsonPrimitive?.content ?: "")
                    else -> null
                }
            }
        ).takeIf { it.segments.isNotEmpty() }
    }
}