package com.stardust.autojs.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * 悬浮窗权限工具类
 */
object FloatingPermissionV2 {

    /**
     * 检查是否有悬浮窗权限
     */
    fun hasPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 跳转到悬浮窗权限设置界面
     */
    fun toSettings(context: Context) {
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = "package:${context.packageName}".toUri()
            context.startActivity(this)
        }
    }

}