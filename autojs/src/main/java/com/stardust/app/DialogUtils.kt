package com.stardust.app

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Looper
import android.view.Window
import android.view.WindowManager

object DialogUtils {

    @JvmStatic
    fun <T : Dialog> showDialog(dialog: T): T {
        val context = dialog.context

        if (!isActivityContext(context)) {
            val window = dialog.window
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            window?.setType(type)
        }
        if (Looper.getMainLooper() == Looper.myLooper()) {
            dialog.show()
        } else {
            GlobalAppContext.post { dialog.show() }
        }
        return dialog
    }

    @JvmStatic
    fun isActivityContext(context: Context?): Boolean {
        if (context is Activity) return true
        if (context is ContextWrapper) {
            return isActivityContext(context.baseContext)
        }
        return false
    }
}
