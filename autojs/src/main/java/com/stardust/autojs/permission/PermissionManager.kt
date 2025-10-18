package com.stardust.autojs.permission

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.REQUEST_INSTALL_PACKAGES
import android.Manifest.permission.SYSTEM_ALERT_WINDOW
import android.Manifest.permission.WRITE_SETTINGS
import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.stardust.notification.NotificationListenerService

object PermissionManager {

    /**
     * 检查是否有指定权限
     * @param context 上下文
     * @param permission 权限
     */
    @JvmStatic
    fun hasPermission(context: Context, permission: String): Boolean {
        return when (permission) {
            MANAGE_EXTERNAL_STORAGE -> hasStoragePermission()
            SYSTEM_ALERT_WINDOW -> hasOverlayPermission(context)
            PACKAGE_USAGE_STATS -> hasUsageStatsPermission(context)
            REQUEST_INSTALL_PACKAGES -> hasInstallPermission(context)
            WRITE_SETTINGS -> hasWriteSettingsPermission(context)
            //通知权限
            POST_NOTIFICATIONS -> hasNotificationPermission(context)
            else -> context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 申请权限
     * @param context 上下文
     * @param permission 权限
     *
     */
    @JvmStatic
    fun requestPermission(context: Context, permission: String) {
        when (permission) {
            MANAGE_EXTERNAL_STORAGE -> requestStoragePermission(context)
            SYSTEM_ALERT_WINDOW -> requestOverlayPermission(context)
            PACKAGE_USAGE_STATS -> requestUsageStatsPermission(context)
            REQUEST_INSTALL_PACKAGES -> requestInstallPermission(context)
            WRITE_SETTINGS -> requestWriteSettingsPermission(context)
            else -> TODO("未实现 $permission 权限的申请方法")
        }
    }

    /**
     * 撤销权限
     * @param context 上下文
     * @param permission 权限
     */
    fun revokePermission(context: Context, permission: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.revokeSelfPermissionOnKill(permission)
        }
    }


    /**
     * 统一检查通知权限
     * @param context  Context
     * @param channelId 要检查的渠道 ID；空字符串时仅检查应用总开关
     * @return true 表示可以弹出通知（总开关 & 渠道开关均通过）
     */
    fun hasNotificationPermission(context: Context, channelId: String = ""): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channelId.isNotBlank()) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = nm.getNotificationChannel(channelId)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    /**
     * 是否拥有管理外部存储的权限（Android 11+）
     */
    private fun hasStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return Environment.isExternalStorageManager()
    }

    /**
     * 请求管理外部存储的权限（Android 11+）
     */
    private fun requestStoragePermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        Intent().apply {
            action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            data = "package:${context.packageName}".toUri()
            context.startActivity(this)
        }
    }

    /**
     * 是否拥有悬浮窗权限
     */
    private fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 请求悬浮窗权限
     */
    private fun requestOverlayPermission(context: Context) {
        Intent().apply {
            action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            data = "package:${context.packageName}".toUri()
            context.startActivity(this)
        }
    }

    /**
     * 否拥有使用情况统计权限（Android 5.0+）
     */
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val mode = appOps.unsafeCheckOpNoThrow(
                    OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
                )
                mode == MODE_ALLOWED
            } else {
                @Suppress("DEPRECATION") val mode = appOps.checkOpNoThrow(
                    OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
                )
                mode == MODE_ALLOWED
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 请求使用情况统计权限
     */
    private fun requestUsageStatsPermission(context: Context) {
        Intent().apply {
            action = Settings.ACTION_USAGE_ACCESS_SETTINGS
            data = "package:${context.packageName}".toUri()
            context.startActivity(this)
        }
    }

    /**
     * 检查应用是否拥有安装未知来源应用的权限（Android 8.0+）
     */
    private fun hasInstallPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    /**
     * 请求安装未知来源应用的权限（Android 8.0+）
     */
    private fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        Intent().apply {
            action = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
            data = "package:${context.packageName}".toUri()
            context.startActivity(this)
        }
    }

    /**
     * 是否拥有修改系统设置的权限
     */
    private fun hasWriteSettingsPermission(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * 请求修改系统设置的权限
     */
    private fun requestWriteSettingsPermission(context: Context) {
        Intent().apply {
            action = Settings.ACTION_MANAGE_WRITE_SETTINGS
            data = "package:${context.packageName}".toUri()
            context.startActivity(this)
        }
    }

    /**
     * 检查应用是否拥有通知访问权限
     */
    @JvmStatic
    fun hasNotificationListenerAccess(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    /**
     * 通知读取权限设置
     */
    @JvmStatic
    fun openNotificationListenerSettings(context: Context) {
        val intent = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val intent1 = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        ComponentName(
                            context,
                            NotificationListenerService::class.java
                        ).flattenToString()
                    )
                }
                val componentName = intent1.resolveActivity(context.packageManager)
                if (componentName != null) {
                    intent1
                } else {
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                }
            }

            else -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
        context.startActivity(intent)
    }

    /**
     * 跳转到应用详情页（兜底方案）
     */
    @JvmStatic
    fun goToAppDetailSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    //    小米自启动intent
    fun xiaomiAutoStart(context: Context): Boolean {
        if (isXiaomi.not()) return false
        val intent = Intent().apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        }
        context.startActivity(intent)
        return true
    }

    fun toSettings(context: Context) {
        when {
            isXiaomi -> {
                try {
                    context.startActivity(xiaomiIntent(context))
                } catch (_: Exception) {
                    context.startActivity(androidIntent(context))
                }
            }

            else -> context.startActivity(androidIntent(context))
        }
    }

    private fun androidIntent(context: Context): Intent {
        return Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = "package:${context.packageName}".toUri()
        }
    }

    private fun xiaomiIntent(context: Context): Intent {
        return Intent().apply {
            action = "miui.intent.action.APP_PERM_EDITOR"
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            putExtra("extra_pkgname", context.packageName)
        }
    }

    private val isXiaomi: Boolean
        get() = "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)

}