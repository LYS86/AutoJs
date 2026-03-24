package com.stardust.autojs.permission

import android.Manifest
import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import timber.log.Timber

object PermissionManager {

    internal var pendingCallback: ((Boolean) -> Unit) = {}
    internal var pendingMultipleCallback: ((Map<String, Boolean>) -> Unit) = {}
    internal var pendingSpecialCallback: ((Boolean) -> Unit) = {}

    @JvmStatic
    fun checkCompat(context: Context, permission: String): Boolean {
        return when (permission) {
            MANAGE_EXTERNAL_STORAGE -> hasStorage(context)
            POST_NOTIFICATIONS -> NotificationManagerCompat.from(context).areNotificationsEnabled()
            else -> ContextCompat.checkSelfPermission(
                context, permission
            ) == PackageManager.PERMISSION_GRANTED
        }
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

    fun requestSpecial(
        context: Context, permission: String, callback: (Boolean) -> Unit = {}
    ) {
        if (checkCompat(context, permission)) {
            callback(true)
            return
        }
        pendingSpecialCallback = callback
        context.startActivity(specialIntent(context, permission))
    }

    private fun runtimeIntent(context: Context, permission: String): Intent {
        return Intent(
            context, PermissionActivity::class.java
        ).putExtra(PermissionActivity.EXTRA_PERMISSION, permission).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun runtimeMultipleIntent(context: Context, permissions: Array<String>): Intent {
        return Intent(
            context, PermissionActivity::class.java
        ).putExtra(PermissionActivity.EXTRA_PERMISSIONS, permissions).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun specialIntent(context: Context, permission: String): Intent {
        return Intent(
            context, PermissionActivity::class.java
        ).putExtra(PermissionActivity.EXTRA_SPECIAL_PERMISSION, permission).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    internal fun onResult(granted: Boolean) {
        pendingCallback.invoke(granted)
        pendingCallback = {}
    }

    internal fun onMultipleResult(result: Map<String, Boolean>) {
        pendingMultipleCallback.invoke(result)
        pendingMultipleCallback = {}
    }

    internal fun onSpecialResult(granted: Boolean) {
        pendingSpecialCallback.invoke(granted)
        pendingSpecialCallback = {}
    }


    fun settingsIntent(context: Context, permission: String): Intent {
        val uri = "package:${context.packageName}".toUri()
        return when (permission) {
            MANAGE_EXTERNAL_STORAGE -> Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri
            )
            POST_NOTIFICATIONS -> notificationIntent(context)
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
        Timber.d("xiaoMiIntent")
        return Intent().apply {
            action = "miui.intent.action.APP_PERM_EDITOR"
            setClassName(
                "com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity"
            )
            putExtra("extra_pkgname", context.packageName)
        }
    }

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

    @JvmOverloads
    fun requestNotification(context: Context, callback: (Boolean) -> Unit = {}) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                requestRuntime(context, POST_NOTIFICATIONS, callback)
            }

            else -> {
                requestSpecial(context, POST_NOTIFICATIONS, callback)
            }
        }
    }

    private fun notificationIntent(context: Context): Intent {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }

            else -> appDetailsIntent(context)
        }
    }

    private fun hasStorage(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> checkCompat(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            else -> Environment.isExternalStorageManager()
        }
    }
}
