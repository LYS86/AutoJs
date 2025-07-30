package org.autojs.autojs.github.model

import android.content.Context
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GitHub API 请求速率限制信息
 * @property limit 总请求次数限制
 * @property remaining 剩余请求次数
 * @property reset 重置时间戳（秒）
 * @property used 已使用次数
 */
data class RateLimit(
    val limit: Int = 0,
    val remaining: Int = 0,
    val reset: Long = 0,
    val used: Int = 0
) {
    /**
     * 获取格式化后的重置时间（本地时间字符串）
     */
    fun resetTime(context: Context): String = reset
        .takeIf { it > 0 }
        ?.let { timestamp ->
            DateUtils.formatDateTime(
                context,
                timestamp * 1000L,
                DateUtils.FORMAT_SHOW_DATE or
                        DateUtils.FORMAT_SHOW_TIME or
                        DateUtils.FORMAT_SHOW_YEAR
            )
        } ?: "0"
}