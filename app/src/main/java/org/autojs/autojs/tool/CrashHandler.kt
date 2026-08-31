package org.autojs.autojs.tool

import android.content.Intent
import android.os.Build
import android.os.Looper
import android.util.Log
import com.stardust.app.GlobalAppContext
import com.stardust.view.accessibility.AccessibilityService
import com.tencent.bugly.crashreport.BuglyLog
import com.tencent.bugly.crashreport.CrashReport
import org.autojs.autojs.BuildConfig
import org.mozilla.javascript.RhinoException
import java.lang.Thread.UncaughtExceptionHandler

class CrashHandler(private val errorReportClass: Class<*>) :
    CrashReport.CrashHandleCallback(), UncaughtExceptionHandler {

    private var mBuglyHandler: UncaughtExceptionHandler? = null
    private val mSystemHandler: UncaughtExceptionHandler =
        checkNotNull(Thread.getDefaultUncaughtExceptionHandler())

    fun setBuglyHandler(buglyHandler: UncaughtExceptionHandler?) {
        mBuglyHandler = buglyHandler
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        Log.e(TAG, "Uncaught Exception", ex)
        if (thread != mainThread) {
            if (ex !is RhinoException) {
                CrashReport.postCatchedException(ex, thread)
            }
            return
        }
        val service = AccessibilityService.instance
        if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG, "disable service: $service")
            service.disableSelf()
        } else {
            BuglyLog.d(TAG, "cannot disable service: $service")
        }
        if (BuildConfig.DEBUG) {
            mSystemHandler.uncaughtException(thread, ex)
        } else {
            mBuglyHandler!!.uncaughtException(thread, ex)
        }
    }

    @Synchronized
    override fun onCrashHandleStart(
        crashType: Int,
        errorType: String?,
        errorMessage: String?,
        errorStack: String?
    ): Map<String, String>? {
        Log.d(
            TAG,
            "onCrashHandleStart: crashType = $crashType, errorType = $errorType, msg = $errorMessage, stack = $errorStack"
        )
        try {
            if (crashType != CRASHTYPE_ANR && !crashTooManyTimes()) {
                startErrorReportActivity("$errorType: $errorMessage", errorStack)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "onCrashHandleStart", e)
        }
        return super.onCrashHandleStart(crashType, errorType, errorMessage, errorStack)
    }

    private fun startErrorReportActivity(msg: String?, detail: String?) {
        val context = GlobalAppContext.get()
        val intent = Intent(context, errorReportClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("message", msg)
            putExtra("error", detail)
        }
        context.startActivity(intent)
    }

    private fun crashTooManyTimes(): Boolean {
        if (crashIntervalTooLong()) {
            resetCrashCount()
            return false
        }
        crashCount++
        return crashCount >= 5
    }

    private fun resetCrashCount() {
        firstCrashMillis = System.currentTimeMillis()
        crashCount = 0
    }

    private fun crashIntervalTooLong(): Boolean {
        return System.currentTimeMillis() - firstCrashMillis > 3000
    }

    companion object {
        private const val TAG = "CrashHandler"
        private val mainThread: Thread = Looper.getMainLooper().thread
        private var crashCount = 0
        private var firstCrashMillis: Long = 0
    }
}
