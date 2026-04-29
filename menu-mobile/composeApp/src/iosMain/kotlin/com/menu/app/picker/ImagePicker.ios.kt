package com.menu.app.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberImagePickerLauncher(
    onSingleResult: (PickedImage?) -> Unit,
    onMultipleResult: (List<PickedImage>) -> Unit
): ImagePickerLauncher {
    // iOS 实现暂用空壳，后续可接入 PHPickerViewController
    return remember {
        object : ImagePickerLauncher {
            override fun pickSingle() {
                onSingleResult(null)
            }

            override fun pickMultiple() {
                onMultipleResult(emptyList())
            }
        }
    }
}
