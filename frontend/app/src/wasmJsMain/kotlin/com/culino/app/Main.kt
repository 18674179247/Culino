package com.culino.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.ComposeViewport
import com.culino.common.util.Constants
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import kotlin.js.Promise

@JsFun("(url) => fetch(url).then(r => r.arrayBuffer())")
private external fun fetchArrayBuffer(url: String): Promise<ArrayBuffer>

private fun ArrayBuffer.toByteArray(): ByteArray {
    val view = Int8Array(this)
    return ByteArray(view.length) { view[it] }
}

/**
 * 决定 API base URL:
 * - 开发环境(localhost / 127.0.0.1 / 局域网 IP 访问时):直接打开发后端 http://<host>:3000/api/v1/
 * - 生产环境:同源 /api/v1/,配合反代使用
 */
private fun resolveApiBaseUrl(): String {
    val loc = window.location
    val host = loc.hostname
    val isDev = host == "localhost" || host == "127.0.0.1" ||
        host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")
    return if (isDev) "http://$host:3000/api/v1/" else "/api/v1/"
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    Constants.API_BASE_URL = resolveApiBaseUrl()

    val fontFamilyState = mutableStateOf<FontFamily?>(null)

    CoroutineScope(Dispatchers.Default).launch {
        runCatching {
            val bytes = fetchArrayBuffer("NotoSansSC-Regular.ttf").await<ArrayBuffer>().toByteArray()
            fontFamilyState.value = FontFamily(Font(identity = "NotoSansSC", data = bytes))
        }
    }

    val root = document.getElementById("root") ?: document.body!!
    ComposeViewport(root) {
        App(dataStorePath = "", fontFamily = fontFamilyState.value)
    }
}
