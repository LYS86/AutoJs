package com.stardust.autojs.permission

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object PermissionManager {

    private var pendingCallback: ((Boolean) -> Unit)? = null

    @JvmStatic
    fun checkCompat(context: Context, permission: String): Boolean {
        return when (permission) {
            MANAGE_EXTERNAL_STORAGE -> hasStorage()
            else -> ContextCompat.checkSelfPermission(
                context, permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @JvmStatic
    fun requestRuntime(context: Context, permission: String, callback: (Boolean) -> Unit) {
        if (checkCompat(context, permission)) {
            callback(true)
            return
        }

        pendingCallback = callback
        context.startActivity(runtimeIntent(context, permission))
    }

    @JvmStatic
    fun requestSpecial(context: Context, permission: String) {
        context.startActivity(specialIntent(context, permission))
    }

    private fun runtimeIntent(context: Context, permission: String): Intent {
        return Intent(
            context, PermissionActivity::class.java
        ).putExtra(PermissionActivity.EXTRA_PERMISSION, permission)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun specialIntent(context: Context, permission: String): Intent {
        return when (permission) {
            MANAGE_EXTERNAL_STORAGE -> manageStorageIntent(context)
            else -> appSettingsIntent(context)
        }
    }

    private fun manageStorageIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            "package:${context.packageName}".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun appSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    internal fun onResult(granted: Boolean) {
        pendingCallback?.invoke(granted)
        pendingCallback = null
    }

    fun revoke(context: Context, permission: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        context.revokeSelfPermissionOnKill(permission)
    }

    private fun hasStorage(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return Environment.isExternalStorageManager()
    }

    private val isXiaomi: Boolean
        get() = "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)
}
