package org.ntqqrev.acidify

import io.ktor.util.decodeBase64String
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.ntqqrev.acidify.util.UrlSignProvider

val defaultSignProvider = UrlSignProvider(
    "aHR0cHM6Ly9hcGkubnRxcXJldi5vcmcvc2lnbi8zOTAzOA==".decodeBase64String()
)
val defaultScope = CoroutineScope(Dispatchers.IO)
val sessionStorePath = Path("acidify-core-test-data", "session.json").also {
    SystemFileSystem.createDirectories(it.parent!!)
}