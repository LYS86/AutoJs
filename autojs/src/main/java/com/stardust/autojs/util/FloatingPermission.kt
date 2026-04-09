package com.stardust.autojs.util

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.stardust.autojs.R
import com.stardust.autojs.permission.PermissionManager
import timber.log.Timber

/**
 * Created by Stardust on 2018/1/30.
 */
object FloatingPermission {

    @JvmStatic
    @Throws(InterruptedException::class)
    fun waitForPermissionGranted(context: Context) {
        if (canDrawOverlays(context)) {
            return
        }
        val action = Runnable {
            manageDrawOverlays(context)
            Toast.makeText(context, R.string.text_no_floating_window_permission, Toast.LENGTH_SHORT).show()
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post(action)
        } else {
            action.run()
        }
        while (!canDrawOverlays(context)) {
            Thread.sleep(200)
        }
    }

    @JvmStatic
    fun manageDrawOverlays(context: Context) {
        val intent = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> PermissionManager.overlaySettingsIntent(context)
            else -> PermissionManager.appDetailsIntent(context)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    @JvmStatic
    fun canDrawOverlays(context: Context): Boolean {
        return PermissionManager.canDrawOverlays(context)
    }
}