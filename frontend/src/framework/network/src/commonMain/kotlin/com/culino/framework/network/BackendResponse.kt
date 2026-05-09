package com.culino.framework.network

import kotlinx.serialization.Serializable

/**
 * 后端统一响应格式
 * 成功: {"ok": true, "data": ..., "error": null}
 * 失败: {"ok": false, "data": null, "error": {"code": "...", "message": "..."}}
 */
@Serializable
data class BackendResponse<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null
)

@Serializable
data class ErrorDetail(
    val code: String,
    val message: String
)
