package org.ntqqrev.acidify.milky.api

class MilkyApiHandler<T : Any, R : Any>(
    val path: String,
    val callHandler: suspend MilkyApiContext.(payload: T) -> R,
)