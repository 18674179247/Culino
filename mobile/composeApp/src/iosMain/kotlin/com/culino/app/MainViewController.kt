package com.culino.app

import androidx.compose.ui.window.ComposeUIViewController
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun MainViewController() = ComposeUIViewController(
    configure = { Napier.base(DebugAntilog()) }
) {
    val dir = NSFileManager.defaultManager.URLForDirectory(
        NSDocumentDirectory, NSUserDomainMask, null, true, null
    )!!.path!!
    App(dataStorePath = dir)
}
