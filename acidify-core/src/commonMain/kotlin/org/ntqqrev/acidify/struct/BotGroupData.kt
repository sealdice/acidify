package org.ntqqrev.acidify.struct

import kotlin.js.JsExport

/**
 * Bot 群数据
 * @property uin 群的 uin（群号）
 * @property name 群名称
 * @property memberCount 群成员数量
 * @property capacity 群容量
 */
@JsExport
class BotGroupData internal constructor(
    val uin: Long,
    val name: String,
    val memberCount: Int,
    val capacity: Int,
)