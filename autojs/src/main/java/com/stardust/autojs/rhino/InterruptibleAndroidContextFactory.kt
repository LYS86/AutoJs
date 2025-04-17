package com.stardust.autojs.rhino

import android.os.Looper
import android.util.Log
import com.stardust.autojs.runtime.exception.ScriptInterruptedException
import org.mozilla.javascript.Context
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Create a new factory. It will cache generated code in the given directory
 *
 * @param cacheDirectory the cache directory
 */
class InterruptibleAndroidContextFactory(
    cacheDirectory: File
) : AndroidContextFactory(cacheDirectory) {

    private val mContextCount = AtomicInteger()
    private val LOG_TAG = "ContextFactory"

    override fun observeInstructionCount(cx: Context, instructionCount: Int) {
        if (Thread.currentThread().isInterrupted && Looper.myLooper() != Looper.getMainLooper()) {
            throw ScriptInterruptedException()
        }
    }

    override fun makeContext(): Context {
        val cx = AutoJsContext(this)
        cx.setInstructionObserverThreshold(10000)
        return cx
    }

    override fun onContextCreated(cx: Context) {
        super.onContextCreated(cx)
        val i = mContextCount.incrementAndGet()
        Log.d(LOG_TAG, "onContextCreated: count = $i")
    }

    override fun onContextReleased(cx: Context) {
        super.onContextReleased(cx)
        val i = mContextCount.decrementAndGet()
        Log.d(LOG_TAG, "onContextReleased: count = $i")
    }
}