package com.culino.common.model

import kotlinx.serialization.Serializable

/**
 * 后端统一响应 DTO(ok / data / error)。
 * 注意:框架层的 sealed ApiResponse 用于封装 HttpClient 结果,
 * 这是后端直接反序列化出来的载荷。
 */
@Serializable
data class ApiResponse<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: String? = null
)
