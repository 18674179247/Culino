package com.culino.common.api

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val url: String
)
