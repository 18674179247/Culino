package com.menu.app.picker

data class PickedImage(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String
)
