package com.stardust.autojs.permission

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.stardust.autojs.permission.PermissionManager.GET_INSTALLED_APPS
import timber.log.Timber

object PermissionManager {

    /**
     * 获取应用列表权限,MIUI 专属
     */
    const val GET_INSTALLED_APPS = "com.android.permission.GET_INSTALLED_APPS"

    internal var pendingCallback: ((Boolean) -> Unit) = {}
    internal var pendingMultipleCallback: ((Map<String, Boolean>) -> Unit) = {}
    internal var pendingSettingsCallback: ((Boolean) -> Unit) = {}
    internal var pendingMediaProjectionCallback: ((Intent?) -> Unit) = {}

    @JvmStatic
    fun checkCompat(context: Context, permission: String): Boolean {
        return when (permission) {
            MANAGE_EXTERNAL_STORAGE -> hasStorage(context)
            POST_NOTIFICATIONS -> NotificationManagerCompat.from(context).areNotificationsEnabled()
            PACKAGE_USAGE_STATS -> checkUsageStatsOp(context)
            else -> ContextCompat.checkSelfPermission(
                context, permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 检查通知权限（包含渠道检查）
     * @param channelId 通知渠道ID，为空时只检查总开关
     */
    fun checkNotificationCompat(context: Context, channelId: String = ""): Boolean {
        val isEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!isEnabled) return false
        if (channelId.isNotBlank()) return checkNotificationChannel(context, channelId)
        return true
    }

    @JvmStatic
    fun getPermissionsNeedToRequest(context: Context, permissions: Array<String>): Array<String> {
        return permissions.map { normalizePermission(it) }.filter { !checkCompat(context, it) }.toTypedArray()
    }

    private fun normalizePermission(permission: String): String {
        return if (permission.startsWith("android.permission.")) {
            permission
        } else {
            "android.permission.${permission.uppercase()}"
        }
    }

    fun requestRuntime(context: Context, permission: String, callback: (Boolean) -> Unit = {}) {
        if (checkCompat(context, permission)) {
            callback(true)
            return
        }

        pendingCallback = callback
        context.startActivity(runtimeIntent(context, permission))
    }

    @JvmStatic
    @JvmOverloads
    fun requestRuntimeMultiple(
        context: Context, permissions: Array<String>, callback: ((Map<String, Boolean>) -> Unit) = {}
    ) {
        val needRequest = permissions.filter { !checkCompat(context, it) }.toTypedArray()

        if (needRequest.isEmpty()) {
            callback.invoke(permissions.associateWith { true })
            return
        }

        pendingMultipleCallback = callback
        context.startActivity(runtimeMultipleIntent(context, needRequest))
    }

    /**
     * 打开特殊权限的设置页面
     * @param permission 权限名称
     * @param channelId 通知渠道ID（仅通知权限有效）
     */
    fun openSettings(
        context: Context, permission: String, channelId: String = "", callback: (Boolean) -> Unit = {}
    ) {
        pendingSettingsCallback = callback
        context.startActivity(settingsLaunchIntent(context, permission, channelId))
    }

    private fun runtimeIntent(context: Context, permission: String): Intent {
        return Intent(
            context, PermissionActivity::class.java
        ).putExtra(PermissionActivity.EXTRA_PERMISSION, permission)
    }

    private fun runtimeMultipleIntent(context: Context, permissions: Array<String>): Intent {
        return Intent(
            context, PermissionActivity::class.java
        ).putExtra(PermissionActivity.EXTRA_PERMISSIONS, permissions)
    }

    private fun settingsLaunchIntent(context: Context, permission: String, channelId: String): Intent {
        return Intent(
            context, PermissionActivity::class.java
        ).putExtra(PermissionActivity.EXTRA_SETTINGS_PERMISSION, permission)
            .putExtra(PermissionActivity.EXTRA_CHANNEL_ID, channelId)
    }

    internal fun onResult(granted: Boolean) {
        pendingCallback.invoke(granted)
        pendingCallback = {}
    }

    internal fun onMultipleResult(result: Map<String, Boolean>) {
        pendingMultipleCallback.invoke(result)
        pendingMultipleCallback = {}
    }

    internal fun onSettingsResult(granted: Boolean) {
        pendingSettingsCallback.invoke(granted)
        pendingSettingsCallback = {}
    }

    internal fun onMediaProjectionResult(data: Intent?) {
        pendingMediaProjectionCallback.invoke(data)
        pendingMediaProjectionCallback = {}
    }

    fun requestMediaProjection(context: Context, callback: (Intent?) -> Unit) {
        pendingMediaProjectionCallback = callback
        context.startActivity(mediaProjectionIntent(context))
    }

    private fun mediaProjectionIntent(context: Context): Intent {
        val intent =
            Intent(context, PermissionActivity::class.java).putExtra(PermissionActivity.EXTRA_MEDIA_PROJECTION, true)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return intent
    }

    fun settingsIntent(context: Context, permission: String, channelId: String = ""): Intent {
        val uri = "package:${context.packageName}".toUri()
        return when (permission) {
            MANAGE_EXTERNAL_STORAGE -> Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri
            )

            POST_NOTIFICATIONS -> notificationIntent(context, channelId)
            PACKAGE_USAGE_STATS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            else -> appSettingsIntent(context)
        }
    }

    fun appSettingsIntent(context: Context): Intent {
        return when {
            isXiaomi -> xiaoMiIntent(context)
            else -> appDetailsIntent(context)
        }
    }

    fun xiaoMiIntent(context: Context): Intent {
        return Intent().apply {
            action = "miui.intent.action.APP_PERM_EDITOR"
            setClassName(
                "com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            putExtra("extra_pkgname", context.packageName)
        }
    }

    @JvmStatic
    fun appDetailsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()
        )
    }

    val isXiaomi: Boolean
        get() = "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)

    fun revoke(context: Context, permission: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        context.revokeSelfPermissionOnKill(permission)
    }

    private fun requestNotificationLegacy(
        context: Context, permission: String, channelId: String, callback: (Boolean) -> Unit
    ) {
        when {
            !checkCompat(context, permission) -> {
                openSettings(context, permission, "") {
                    requestNotificationLegacy(context, permission, channelId, callback)
                }
            }

            channelId.isNotBlank() && !checkNotificationChannel(context, channelId) -> {
                openSettings(context, permission, channelId, callback)
            }

            else -> {
                callback(true)
            }
        }
    }

    @SuppressLint("InlinedApi")
    @JvmOverloads
    fun requestNotification(context: Context, channelId: String, callback: (Boolean) -> Unit = {}) {
        val permission = POST_NOTIFICATIONS
        if (checkNotificationCompat(context, channelId)) {
            callback(true)
            return
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                requestRuntime(context, permission) { isGranted ->
                    if (!isGranted) return@requestRuntime callback(false)
                    openSettings(context, permission, channelId, callback)
                }
            }

            else -> {
                requestNotificationLegacy(context, permission, channelId, callback)
            }
        }
    }

    fun checkNotificationChannel(
        context: Context,
        channelId: String,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = NotificationManagerCompat.from(context)
        val channel = manager.getNotificationChannel(channelId) ?: return true
        return channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun notificationChannelIntent(context: Context, channelId: String): Intent {
        return Intent().apply {
            action = Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }

    private fun notificationIntent(context: Context, channelId: String): Intent {
        return when {
            channelId.isNotBlank() -> notificationChannelIntent(context, channelId)
            else -> Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }
    }

    private fun hasStorage(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> checkCompat(
                context, WRITE_EXTERNAL_STORAGE
            )

            else -> Environment.isExternalStorageManager()
        }
    }

    /**
     * 检查当前系统是否支持 [GET_INSTALLED_APPS] 权限。
     *
     * 影响版本：MIUI 13 及以上系统版本
     *
     * @see <a href="https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1619">应用列表权限的适配说明</a>
     */
    private fun isSupportGetInstalledAppsPermission(context: Context): Boolean {
        return try {
            val permissionInfo = context.packageManager.getPermissionInfo(
                GET_INSTALLED_APPS, 0
            )
            permissionInfo.packageName.equals("com.lbe.security.miui", ignoreCase = true)
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    @JvmStatic
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    @JvmStatic
    fun canWriteSettings(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    private fun checkUsageStatsOp(context: Context): Boolean {
        Timber.tag("DrawerFragment").d("checkUsageStatsOp: ${context.packageName}")
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
        )

        return when {
            mode == AppOpsManager.MODE_DEFAULT -> {
                context.checkCallingOrSelfPermission(PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
            }

            else -> {
                mode == AppOpsManager.MODE_ALLOWED
            }
        }
    }

    @JvmStatic
    fun overlaySettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
