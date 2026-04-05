package org.ntqqrev.yogurt.util

import io.ktor.utils.io.errors.PosixException

actual tailrec fun Throwable.isCausedByAddrInUse(): Boolean {
    if (this is PosixException.AddressAlreadyInUseException) return true
    if (this is PosixException.PosixErrnoException && this.errno == 98) return true
    return cause?.isCausedByAddrInUse() ?: false
}
