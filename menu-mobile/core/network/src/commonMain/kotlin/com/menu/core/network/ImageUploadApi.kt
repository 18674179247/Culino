package com.menu.core.network

import com.menu.core.common.AppResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

class ImageUploadApi(private val httpClient: HttpClient) {

    suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        contentType: String
    ): AppResult<String> {
        return try {
            val response: HttpResponse = httpClient.submitFormWithBinaryData(
                url = "upload/image",
                formData = formData {
                    append("file", bytes, Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }
            )
            if (response.status.value in 200..299) {
                val body = response.body<BackendResponse<UploadResponse>>()
                if (body.ok && body.data != null) {
                    AppResult.Success(body.data.url)
                } else {
                    AppResult.Error(body.error?.message ?: "上传失败")
                }
            } else {
                AppResult.Error("上传失败: HTTP ${response.status.value}")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "网络错误", e)
        }
    }
}
