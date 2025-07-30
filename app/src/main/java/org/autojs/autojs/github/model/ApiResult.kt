package org.autojs.autojs.github.model

/**
 * API结果密封类
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T, val rateLimit: RateLimit) : ApiResult<T>()
    data class Error(val error: ApiError) : ApiResult<Nothing>()
}