package org.ntqqrev.yogurt.util

import io.ktor.utils.io.errors.PosixException

private val addrInUsePosixCodes = setOf(
    48,
    98,
    10048,
)

actual tailrec fun Throwable.isCausedByAddrInUse(): Boolean {
    if (this is PosixException.AddressAlreadyInUseException) {
        return true
    }
    if (this is PosixException.PosixErrnoException && addrInUsePosixCodes.contains(this.errno)) {
        return true
    }
    return cause?.isCausedByAddrInUse() ?: false
}
