package com.culino.app.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.files.File
import kotlin.js.Promise

@JsFun("(blob) => blob.arrayBuffer()")
private external fun blobArrayBuffer(blob: File): Promise<ArrayBuffer>

@JsFun("() => Date.now()")
private external fun nowMillis(): Double

private fun ArrayBuffer.toByteArray(): ByteArray {
    val view = Int8Array(this)
    return ByteArray(view.length) { view[it] }
}

private suspend fun File.toPickedImage(): PickedImage {
    val buffer = blobArrayBuffer(this).await<ArrayBuffer>()
    val bytes = buffer.toByteArray()
    val mime = type.ifBlank { "image/jpeg" }
    val ext = when {
        mime.contains("png") -> "png"
        mime.contains("webp") -> "webp"
        mime.contains("gif") -> "gif"
        else -> "jpg"
    }
    val fileName = name.ifBlank { "image_${nowMillis().toLong()}.$ext" }
    return PickedImage(bytes = bytes, fileName = fileName, contentType = mime)
}

@Composable
actual fun rememberImagePickerLauncher(
    onSingleResult: (PickedImage?) -> Unit,
    onMultipleResult: (List<PickedImage>) -> Unit
): ImagePickerLauncher {
    val scope = remember { CoroutineScope(Dispatchers.Default) }
    return remember {
        object : ImagePickerLauncher {
            override fun pickSingle() {
                val input = document.createElement("input") as HTMLInputElement
                input.type = "file"
                input.accept = "image/*"
                input.addEventListener("change", { _: Event ->
                    val file = input.files?.item(0)
                    if (file != null) {
                        scope.launch { onSingleResult(file.toPickedImage()) }
                    } else {
                        onSingleResult(null)
                    }
                })
                input.click()
            }

            override fun pickMultiple() {
                val input = document.createElement("input") as HTMLInputElement
                input.type = "file"
                input.accept = "image/*"
                input.multiple = true
                input.addEventListener("change", { _: Event ->
                    val files = input.files
                    if (files != null) {
                        scope.launch {
                            val list = buildList {
                                for (i in 0 until files.length) {
                                    files.item(i)?.let { add(it.toPickedImage()) }
                                }
                            }
                            onMultipleResult(list)
                        }
                    }
                })
                input.click()
            }
        }
    }
}
