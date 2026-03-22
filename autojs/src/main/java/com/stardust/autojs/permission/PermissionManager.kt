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

    internal var pendingCallback: ((Boolean) -> Unit)? = null
    internal var pendingMultipleCallback: ((Map<String, Boolean>) -> Unit)? = null

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
    fun getPermissionsNeedToRequest(context: Context, permissions: Array<String>): Array<String> {
        return permissions
            .map { normalizePermission(it) }
            .filter { !checkCompat(context, it) }
            .toTypedArray()
    }

    private fun normalizePermission(permission: String): String {
        return if (permission.startsWith("android.permission.")) {
            permission
        } else {
            "android.permission.${permission.uppercase()}"
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
    @JvmOverloads
    fun requestRuntimeMultiple(
        context: Context,
        permissions: Array<String>,
        callback: ((Map<String, Boolean>) -> Unit)? = null
    ) {
        val needRequest = permissions.filter { !checkCompat(context, it) }.toTypedArray()

        if (needRequest.isEmpty()) {
            callback?.invoke(permissions.associateWith { true })
            return
        }

        pendingMultipleCallback = callback
        context.startActivity(runtimeMultipleIntent(context, needRequest))
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

    private fun runtimeMultipleIntent(context: Context, permissions: Array<String>): Intent {
        return Intent(
            context, PermissionActivity::class.java
        ).putExtra(PermissionActivity.EXTRA_PERMISSIONS, permissions)
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

    internal fun onMultipleResult(result: Map<String, Boolean>) {
        pendingMultipleCallback?.invoke(result)
        pendingMultipleCallback = null
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
