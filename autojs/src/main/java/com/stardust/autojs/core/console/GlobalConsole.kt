package com.stardust.autojs.core.console

import android.util.Log
import com.stardust.util.UiHandler
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * Created by Stardust on 2017/10/22.
 */
open class GlobalConsole(uiHandler: UiHandler) : ConsoleImpl(uiHandler) {

    private fun createDateFormat(): SimpleDateFormat =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    override fun println(level: Int, charSequence: CharSequence): String {
        val dateFormat = createDateFormat()
        val log = String.format(
            Locale.getDefault(), "%s/%s: %s",
            dateFormat.format(Date()), getLevelChar(level), charSequence.toString()
        )
        logToTimber(level, log)
        super.println(level, log)
        return log
    }

    private fun logToTimber(level: Int, message: String) {
        when (level) {
            Log.VERBOSE -> Timber.v(message)
            Log.DEBUG -> Timber.d(message)
            Log.INFO -> Timber.i(message)
            Log.WARN -> Timber.w(message)
            Log.ERROR -> Timber.e(message)
            Log.ASSERT -> Timber.wtf(message)
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
            else -> ""
        }
    }

}
