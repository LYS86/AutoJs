package com.stardust.autojs.util

import android.Manifest.permission.SYSTEM_ALERT_WINDOW
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.stardust.autojs.R
import com.stardust.autojs.permission.PermissionManager
import timber.log.Timber

/**
 * 悬浮窗权限工具类
 * 用于检查、申请和管理悬浮窗（SYSTEM_ALERT_WINDOW）权限
 */
object FloatingPermission {

    /**
     * 确保悬浮窗权限已授予
     * 如果未授予，将弹出提示并尝试引导用户前往设置页面
     *
     * @param context 上下文
     * @return 是否已拥有悬浮窗权限
     */
    @JvmStatic
    fun ensurePermissionGranted(context: Context): Boolean {
        if (!canDrawOverlays(context)) {
            Toast.makeText(context, R.string.text_no_floating_window_permission, Toast.LENGTH_SHORT)
                .show()
            manageDrawOverlays(context)
            return false
        }
        return true
    }

    /**
     * 阻塞当前线程，直到用户授予悬浮窗权限或中断
     * 注意：此方法会阻塞线程，建议在子线程中使用
     *
     * @param context 上下文
     * @throws InterruptedException 如果线程被中断
     */
    @JvmStatic
    @Throws(InterruptedException::class)
    fun waitForPermissionGranted(context: Context) {
        if (canDrawOverlays(context)) return

        val r = Runnable {
            manageDrawOverlays(context)
            Toast.makeText(context, R.string.text_no_floating_window_permission, Toast.LENGTH_SHORT)
                .show()
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post(r)
        } else {
            r.run()
        }

        while (true) {
            if (canDrawOverlays(context)) return
            Thread.sleep(200)
        }
    }

    /**
     * 尝试申请悬浮窗权限
     * 如果申请失败（如异常），将引导用户进入应用详情页手动开启权限
     *
     * @param context 上下文
     */
    @JvmStatic
    fun manageDrawOverlays(context: Context) {
        try {
            PermissionManager.requestPermission(
                context,
                SYSTEM_ALERT_WINDOW
            )
        } catch (ex: Exception) {
            Timber.e(ex)
            goToAppDetailSettings(context)
        }
    }

    private fun goToAppDetailSettings(context: Context) {
        try {
            return PermissionManager.goToAppDetailSettings(context)
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    /**
     * 检查当前是否拥有悬浮窗权限
     *
     * @param context 上下文
     * @return 是否拥有悬浮窗权限
     */
    @JvmStatic
    fun canDrawOverlays(context: Context): Boolean {
        return PermissionManager.hasPermission(
            context,
            SYSTEM_ALERT_WINDOW
        )
    }
}