package org.autojs.autojs.github.model

data class ApiError(
    val code: Int,
    var message: String,
    val rateLimit: RateLimit=RateLimit()
)