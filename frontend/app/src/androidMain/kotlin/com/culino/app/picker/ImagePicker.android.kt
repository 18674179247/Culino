package com.culino.app.picker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePickerLauncher(
    onSingleResult: (PickedImage?) -> Unit,
    onMultipleResult: (List<PickedImage>) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current

    fun uriToPickedImage(uri: Uri): PickedImage? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "image/jpeg"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val ext = when {
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("gif") -> "gif"
            else -> "jpg"
        }
        return PickedImage(
            bytes = bytes,
            fileName = "image_${System.currentTimeMillis()}.$ext",
            contentType = mimeType
        )
    }

    val singleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        onSingleResult(uri?.let { uriToPickedImage(it) })
    }

    val multipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        onMultipleResult(uris.mapNotNull { uriToPickedImage(it) })
    }

    return remember(singleLauncher, multipleLauncher) {
        object : ImagePickerLauncher {
            override fun pickSingle() {
                singleLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }

            override fun pickMultiple() {
                multipleLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }
}
