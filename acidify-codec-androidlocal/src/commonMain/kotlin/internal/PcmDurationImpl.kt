package org.ntqqrev.acidify.codec.internal

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal fun commonPcmDurationImpl(
    input: ByteArray,
    bitDepth: Int,
    channelCount: Int,
    sampleRate: Int,
): Duration {
    val bytesPerSecond = (bitDepth / 8) * channelCount * sampleRate
    return (input.size.toDouble() / bytesPerSecond).seconds
}