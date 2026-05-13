package com.culino.framework.media.picker

data class PickedImage(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String
)
