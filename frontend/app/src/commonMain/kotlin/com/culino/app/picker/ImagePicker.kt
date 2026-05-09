package com.culino.app.picker

import androidx.compose.runtime.Composable

interface ImagePickerLauncher {
    fun pickSingle()
    fun pickMultiple()
}

@Composable
expect fun rememberImagePickerLauncher(
    onSingleResult: (PickedImage?) -> Unit,
    onMultipleResult: (List<PickedImage>) -> Unit
): ImagePickerLauncher
