package com.stardust.autojs.core.console

import android.util.Log
import com.stardust.util.UiHandler
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class GlobalConsole(uiHandler: UiHandler) : ConsoleImpl(uiHandler) {

    override fun println(level: Int, charSequence: CharSequence): String {
        logWithTimber(level, charSequence.toString())
        val formattedLog = formatLog(level, charSequence)
        super.println(level, formattedLog)
        return formattedLog
    }

    private fun formatLog(level: Int, charSequence: CharSequence): String {
        return String.format(
            Locale.getDefault(),
            "%s/%s: %s",
            DATE_FORMAT.format(Date()),
            getLevelChar(level),
            charSequence.toString()
        )
    }

    private fun logWithTimber(level: Int, message: String) {
        when (level) {
            Log.VERBOSE -> Timber.v(message)
            Log.DEBUG -> Timber.d(message)
            Log.INFO -> Timber.i(message)
            Log.WARN -> Timber.w(message)
            Log.ERROR -> Timber.e(message)
            Log.ASSERT -> Timber.wtf(message)
            else -> Timber.d(message)
        }
    }

    private fun getLevelChar(level: Int): String {
        return when (level) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "unknown"
        }
    }

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    }
}
