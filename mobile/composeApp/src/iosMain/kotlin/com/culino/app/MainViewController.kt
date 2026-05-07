package com.culino.app

import androidx.compose.ui.window.ComposeUIViewController
import com.culino.core.common.Constants
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun MainViewController() = ComposeUIViewController(
    configure = {
        Napier.base(DebugAntilog())
        val apiUrl = NSBundle.mainBundle.objectForInfoDictionaryKey("API_BASE_URL") as? String
        Constants.API_BASE_URL = apiUrl ?: "http://127.0.0.1:3000/api/v1/"
    }
) {
    val dir = NSFileManager.defaultManager.URLForDirectory(
        NSDocumentDirectory, NSUserDomainMask, null, true, null
    )!!.path!!
    App(dataStorePath = dir)
}
