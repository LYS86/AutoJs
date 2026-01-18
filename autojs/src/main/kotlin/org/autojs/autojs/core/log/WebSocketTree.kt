package org.autojs.autojs.core.log

import com.stardust.autojs.core.console.GlobalConsole
import timber.log.Timber

/**
 * Timber Tree 实现日志通过 WebSocket 发送到开发插件服务器
 */
class WebSocketTree(private val webSocketSender: WebSocketSender) : Timber.DebugTree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        val isGlobalConsole = tag.equals(GlobalConsole::class.simpleName, ignoreCase = true)
        return isGlobalConsole
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        webSocketSender.sendLog(message)
    }

}

/**
 * WebSocket 日志发送接口
 */
interface WebSocketSender {
    fun sendLog(message: String)
}
