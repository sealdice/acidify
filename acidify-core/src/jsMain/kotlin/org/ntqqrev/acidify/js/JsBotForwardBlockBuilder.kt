package org.ntqqrev.acidify.js

import org.ntqqrev.acidify.message.BotOutgoingMessageBuilder

@JsExport
@JsName("BotForwardBlockBuilder")
@AcidifyJsWrapper
class JsBotForwardBlockBuilder internal constructor(
    val underlying: BotOutgoingMessageBuilder.Forward
) {
    fun node(
        senderUin: Long,
        senderName: String,
        block: (JsBotOutgoingMessageBuilder) -> Unit
    ) {
        underlying.node(
            senderUin,
            senderName
        ) {
            val b = JsBotOutgoingMessageBuilder(this)
            block(b)
        }
    }
}