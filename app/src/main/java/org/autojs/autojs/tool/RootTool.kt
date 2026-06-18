package org.autojs.autojs.tool

import android.util.Log
import com.stardust.autojs.core.util.ProcessShell

object RootTool {

    private const val TAG = "RootTool"

    private const val POINTER_LOCATION_TOGGLE_CMD = "enabled=\$(settings get system pointer_location)\n" +
            "if [[ \$enabled == 1 ]]\n" +
            "then\n" +
            "settings put system pointer_location 0\n" +
            "else\n" +
            "settings put system pointer_location 1\n" +
            "fi\n"

    @JvmStatic
    fun isRootAvailable(): Boolean {
        return try {
            ProcessShell.execCommand("id", true).code == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check root availability", e)
            false
        }
    }

    @JvmStatic
    fun togglePointerLocation() {
        try {
            ProcessShell.execCommand(POINTER_LOCATION_TOGGLE_CMD, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle pointer location", e)
        }
    }

    @JvmStatic
    fun setPointerLocationEnabled(enabled: Boolean) {
        try {
            ProcessShell.execCommand("settings put system pointer_location ${if (enabled) 1 else 0}", true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set pointer location enabled=$enabled", e)
        }
    }
}
