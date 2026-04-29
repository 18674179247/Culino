package com.menu.core.network

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val url: String
)
