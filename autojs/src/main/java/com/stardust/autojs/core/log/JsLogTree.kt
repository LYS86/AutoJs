package com.stardust.autojs.core.log

import timber.log.Timber

class JsLogTree(private val logSink: LogSink) : Timber.DebugTree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return logSink.isLoggable(tag, priority)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        logSink.send(message)
    }
}
