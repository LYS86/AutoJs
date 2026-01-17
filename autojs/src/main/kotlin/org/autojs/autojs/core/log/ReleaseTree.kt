package org.autojs.autojs.core.log

import android.util.Log
import timber.log.Timber.DebugTree

class ReleaseTree : DebugTree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.INFO
    }
}