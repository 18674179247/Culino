package com.culino.framework.network

import com.culino.common.util.AppResult

/**
 * ApiResponse<T> → AppResult<T> 适配。
 * Repository 层只需关心"接口返回 Success/Error 两态",不必关心外层 ApiResponse 形状。
 */
fun <T> ApiResponse<T>.toAppResult(): AppResult<T> = when (this) {
    is ApiResponse.Success -> AppResult.Success(data)
    is ApiResponse.Error -> AppResult.Error(message)
}

/**
 * 包装一次网络调用,把"异常 → AppResult.Error"和"ApiResponse → AppResult"合并为一步。
 *
 * 使用前:
 * ```
 * override suspend fun x() = try {
 *   when (val r = api.x()) {
 *     is ApiResponse.Success -> AppResult.Success(r.data)
 *     is ApiResponse.Error -> AppResult.Error(r.message)
 *   }
 * } catch (e: Exception) { AppResult.Error(e.message ?: "Unknown error", e) }
 * ```
 * 使用后:
 * ```
 * override suspend fun x() = safeApiCall { api.x() }
 * ```
 */
suspend inline fun <T> safeApiCall(block: () -> ApiResponse<T>): AppResult<T> = try {
    block().toAppResult()
} catch (e: Exception) {
    AppResult.Error(e.message ?: "Unknown error", e)
}
