package org.ntqqrev.acidify.milky.api

class MilkyApiException(
    val retcode: Int,
    override val message: String?,
) : Exception()