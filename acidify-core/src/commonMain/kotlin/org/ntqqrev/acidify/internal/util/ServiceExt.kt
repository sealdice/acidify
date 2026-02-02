package org.ntqqrev.acidify.internal.util

import org.ntqqrev.acidify.exception.ServiceInternalException
import org.ntqqrev.acidify.internal.service.Service

internal fun Service<*, *>.checkRetCode(retCode: Int, retMsg: String? = null) {
    if (retCode != 0) {
        throw ServiceInternalException(this, retCode, retMsg ?: "")
    }
}