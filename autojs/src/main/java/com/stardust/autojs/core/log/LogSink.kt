package com.stardust.autojs.core.log

import com.stardust.autojs.core.console.GlobalConsole

abstract class LogSink {

    abstract fun send(message: String)

    open fun isLoggable(tag: String?, priority: Int): Boolean {
        val isGlobalConsole = tag.equals(GlobalConsole::class.simpleName, ignoreCase = true)
        return isGlobalConsole
    }
}
