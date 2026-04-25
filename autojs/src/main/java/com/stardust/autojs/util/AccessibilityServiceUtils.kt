package com.stardust.autojs.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.RequiresPermission
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.core.accessibility.AccessibilityService
import com.stardust.autojs.core.pref.PrefV2
import com.stardust.autojs.core.util.ProcessShell
import com.stardust.autojs.permission.PermissionManager
import com.stardust.autojs.shizuku.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.stardust.view.accessibility.AccessibilityService as AccessibilityService2

object AccessibilityServiceUtils {

    private val serviceClass = AccessibilityService::class.java
    private val serviceName = "${GlobalAppContext.get().packageName}/${serviceClass.name}"
    const val KEY = "auto_enable_service"
    val isAutoEnable: Boolean by PrefV2.boolean(KEY, false)
    const val ACCESSIBILITY = "accessibility"
    private const val PUT_SERVICES =
        "settings put secure enabled_accessibility_services %s;settings put secure accessibility_enabled 1 "

    @JvmStatic
    fun isEnabled(): Boolean {
        val services = readServices() ?: return false
        return services.contains(serviceName)
    }

    /**
     * 启用无障碍服务，按权限优先级自动选择方式：
     *
     * 1. **系统 API**（[android.Manifest.permission.WRITE_SECURE_SETTINGS] 权限）
     * 2. **Shizuku shell**
     * 3. **Root shell**
     */
    suspend fun enableServiceCompat(): Boolean {
        if (isAutoEnable.not()) return false
        val context = GlobalAppContext.get()
        val currentServices = readServices()
        val services = when {
            currentServices.isNullOrBlank() -> serviceName
            currentServices.split(":").any { it.trim() == serviceName } -> currentServices
            else -> "$serviceName:${currentServices.trim()}"
        }
        return when {
            PermissionManager.checkCompat(context, Manifest.permission.WRITE_SECURE_SETTINGS) -> writeServices(services)
            writeServicesByShizuku(services) -> true
            else -> writeServicesByRoot(services)
        }
    }

    suspend fun enableServiceAndWait(timeout: Long = 2000L): Boolean {
        if (enableServiceCompat().not()) return false
        return withContext(Dispatchers.IO) {
            AccessibilityService2.waitForEnabled(timeout)
        }
    }

    @JvmStatic
    fun enableServiceAndWaitBlocking(timeout: Long = 2000L): Boolean {
        return runBlocking { enableServiceAndWait(timeout) }
    }

    @JvmStatic
    fun enableServiceBlocking() {
        runBlocking { enableServiceCompat() }
    }

    fun openSetting(context: Context, callback: (Boolean) -> Unit) {
        PermissionManager.openSettings(context, ACCESSIBILITY) { enabled ->
            callback(enabled)
        }
    }

    @JvmStatic
    fun openSetting() {
        GlobalAppContext.get().startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** 读取已启用的无障碍列表 */
    fun readServices(): String? {
        return Settings.Secure.getString(
            GlobalAppContext.get().contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
    }

    @RequiresPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
    @Throws(SecurityException::class)
    private fun writeServices(value: String): Boolean {
        val resolver = GlobalAppContext.get().contentResolver
        val success1 = Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, value)
        val success2 = Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        return success1 && success2
    }

    private fun writeServicesByRoot(value: String): Boolean {
        val cmd = PUT_SERVICES.format(value)
        return ProcessShell.execCommand(cmd, true).code == 0
    }

    private suspend fun writeServicesByShizuku(value: String): Boolean {
        val cmd = PUT_SERVICES.format(value)
        return Shell.execSuspend(cmd).code == 0
    }
}