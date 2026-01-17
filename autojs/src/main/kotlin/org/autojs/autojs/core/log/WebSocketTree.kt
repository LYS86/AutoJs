package org.autojs.autojs.core.log

import android.util.Log
import com.stardust.autojs.core.console.GlobalConsole
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Timber Tree 实现日志通过 WebSocket 发送到开发插件服务器
 */
class WebSocketTree(private val webSocketSender: WebSocketSender) : Timber.DebugTree() {

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())


    override fun isLoggable(tag: String?, priority: Int): Boolean {
        // 条件：必须是 GlobalConsole 类的日志（忽略大小写）
        val isGlobalConsole = tag.equals(GlobalConsole::class.simpleName, ignoreCase = true)
        Log.d(
            "WebSocketTree",
            "tag : $tag, name : ${GlobalConsole::class.java.name},isGlobalConsole : $isGlobalConsole"
        )
        return isGlobalConsole
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val logMessage = buildLogMessage(priority, tag, message, t)
        webSocketSender.sendLog(logMessage)
    }

    private fun buildLogMessage(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?
    ): String {
        val timestamp = dateFormat.format(Date())
        val priorityChar = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "?"
        }

        val sb = StringBuilder()
        sb.append("$timestamp ")
        sb.append("$priorityChar/")
        sb.append(tag ?: "null")
        sb.append(": $message")
        return sb.toString()
    }
}

/**
 * WebSocket 日志发送接口
 */
interface WebSocketSender {
    fun sendLog(message: String)
}
