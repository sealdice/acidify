package org.ntqqrev.acidify.js

import org.ntqqrev.acidify.struct.BotGroupNotification

@JsExport
@AcidifyJsWrapper
class JsGroupNotifications internal constructor(
    val notifications: Array<BotGroupNotification>,
    val nextSequence: Long?
)