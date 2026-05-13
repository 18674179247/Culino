package com.culino.framework.media.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSItemProvider
import platform.Foundation.timeIntervalSince1970
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, size.toULong())
        }
    }
}

private fun topMostViewController(): UIViewController? {
    val window: UIWindow? = UIApplication.sharedApplication.keyWindow
        ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    var vc = window?.rootViewController
    while (vc?.presentedViewController != null) {
        vc = vc.presentedViewController
    }
    return vc
}

private class PickerDelegate(
    private val onDone: (List<PHPickerResult>) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        onDone(results)
        delegateHolders.remove(this)
    }
}

// PHPickerViewController.delegate 是 weak 引用,必须外部持有强引用直到回调结束,否则会在 ARC 下被释放
private val delegateHolders = mutableSetOf<PickerDelegate>()

private fun typeIdentifierToMime(uti: String): Pair<String, String> = when {
    uti.endsWith("jpeg") || uti.endsWith("jpg") -> "image/jpeg" to "jpg"
    uti.endsWith("png") -> "image/png" to "png"
    uti.endsWith("webp") -> "image/webp" to "webp"
    uti.endsWith("heic") -> "image/heic" to "heic"
    uti.endsWith("gif") -> "image/gif" to "gif"
    else -> "image/jpeg" to "jpg"
}

private suspend fun PHPickerResult.toPickedImage(): PickedImage? {
    val provider: NSItemProvider = itemProvider
    // 优先选常见图片 UTI;若无则回退到 "public.image"
    val preferredUtis = listOf(
        "public.jpeg",
        "public.png",
        "public.webp",
        "public.heic",
        "com.compuserve.gif"
    )
    @Suppress("UNCHECKED_CAST")
    val available = (provider.registeredTypeIdentifiers as List<String>)
    val typeId = preferredUtis.firstOrNull { it in available }
        ?: available.firstOrNull { it.startsWith("public.") && it.contains("image") }
        ?: "public.image"

    val deferred = CompletableDeferred<NSData?>()
    provider.loadDataRepresentationForTypeIdentifier(typeId) { data: NSData?, _: NSError? ->
        deferred.complete(data)
    }
    val data = deferred.await() ?: return null
    val (mime, ext) = typeIdentifierToMime(typeId)
    val ts = NSDate().timeIntervalSince1970.toLong()
    return PickedImage(
        bytes = data.toByteArray(),
        fileName = "image_$ts.$ext",
        contentType = mime
    )
}

@Composable
actual fun rememberImagePickerLauncher(
    onSingleResult: (PickedImage?) -> Unit,
    onMultipleResult: (List<PickedImage>) -> Unit
): ImagePickerLauncher {
    val scope = remember { MainScope() }
    return remember {
        object : ImagePickerLauncher {
            override fun pickSingle() {
                present(selectionLimit = 1) { results ->
                    scope.launch {
                        val image = results.firstOrNull()?.toPickedImage()
                        onSingleResult(image)
                    }
                }
            }

            override fun pickMultiple() {
                present(selectionLimit = 9) { results ->
                    scope.launch {
                        val list = results.mapNotNull { it.toPickedImage() }
                        onMultipleResult(list)
                    }
                }
            }

            private fun present(
                selectionLimit: Long,
                onResults: (List<PHPickerResult>) -> Unit
            ) {
                val config = PHPickerConfiguration().apply {
                    setSelectionLimit(selectionLimit)
                    setFilter(PHPickerFilter.imagesFilter())
                }
                val picker = PHPickerViewController(configuration = config)
                val delegate = PickerDelegate(onDone = onResults)
                delegateHolders.add(delegate)
                picker.delegate = delegate
                topMostViewController()?.presentViewController(
                    picker,
                    animated = true,
                    completion = null
                )
            }
        }
    }
}
