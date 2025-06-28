package org.autojs.autojs.tool

import android.content.ActivityNotFoundException
import android.content.Context
import android.text.TextUtils
import com.shizuku.Utils
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.core.accessibility.AccessibilityService as AccessibilityService1
import com.stardust.view.accessibility.AccessibilityService as AccessibilityService2
import com.stardust.autojs.core.util.ProcessShell
import com.stardust.view.accessibility.AccessibilityServiceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import timber.log.Timber
import java.util.Locale

object AccessibilityServiceTool2 {

    private val sAccessibilityServiceClass = AccessibilityService1::class.java

    fun enableAccessibilityService() {
        if (Pref.shouldEnableAccessibilityServiceByRoot()) {
            if (!enableAccessibilityServiceByRoot(sAccessibilityServiceClass)) {
                goToAccessibilitySetting()
            }
        } else {
            goToAccessibilitySetting()
        }
    }

    fun goToAccessibilitySetting() {
        val context = GlobalAppContext.get()
        if (Pref.isFirstGoToAccessibilitySetting()) {
            GlobalAppContext.toast("${context.getString(R.string.text_please_choose)} ${context.getString(R.string.app_name)}")
        }
        try {
            AccessibilityServiceUtils.goToAccessibilitySetting(context)
        } catch (e: ActivityNotFoundException) {
            GlobalAppContext.toast("${context.getString(R.string.go_to_accessibility_settings)} ${context.getString(R.string.app_name)}")
        }
    }

    private const val cmd = "enabled=$(settings get secure enabled_accessibility_services)\n" +
            "pkg=%s\n" +
            "if [[ \$enabled == *\$pkg* ]]\n" +
            "then\n" +
            "echo already_enabled\n" +
            "else\n" +
            "enabled=\$pkg:\$enabled\n" +
            "settings put secure enabled_accessibility_services \$enabled\n" +
            "fi\n" +
            "settings put secure accessibility_enabled 1"

    fun enableAccessibilityServiceByRoot(accessibilityService: Class<out android.accessibilityservice.AccessibilityService>): Boolean {
        val serviceName = "${GlobalAppContext.get().packageName}/${accessibilityService.name}"
        return try {
            TextUtils.isEmpty(ProcessShell.execCommand(String.format(Locale.getDefault(), cmd, serviceName), true).error)
        } catch (e: Exception) {
            false
        }
    }

    fun byRoot(timeOut: Long): Boolean {
        if (!RootTool.isRootAvailable()) {
            Timber.d("Root not available, skipping accessibility enable")
            return false
        }
        if (enableAccessibilityServiceByRoot(sAccessibilityServiceClass)) {
            return AccessibilityService2.waitForEnabled(timeOut)
        }
        return false
    }

    fun enableAccessibilityServiceByRootIfNeeded() {
        if (AccessibilityService2.instance == null && Pref.shouldEnableAccessibilityServiceByRoot()) {
            enableAccessibilityServiceByRoot(sAccessibilityServiceClass)
        }
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return AccessibilityServiceUtils.isAccessibilityServiceEnabled(context, sAccessibilityServiceClass)
    }

    /**
     * 尝试启动无障碍服务（依次尝试 Shizuku 和 Root 方式）
     * @param timeout 超时时间（毫秒）
     * @return 是否成功启动
     */
    fun start(timeout: Long): Boolean {
        return byShizuku(timeout) || byRoot(timeout)
    }

    // 新增协程版本
    suspend fun start2(timeout: Long): Boolean {
        return byShizuku2(timeout) || withContext(Dispatchers.IO) {
            byRoot(timeout)
        }
    }

    fun byShizuku(timeout: Long): Boolean {
        val context = GlobalAppContext.get()
        return try {
            val pkg = context.packageName
            val cmd = "settings put secure enabled_accessibility_services $pkg/${sAccessibilityServiceClass.name}\n" +
                    "settings put secure accessibility_enabled 1"
            Utils.exec(cmd)
            AccessibilityService2.waitForEnabled(timeout)
        } catch (e: Exception) {
            Timber.e(e, "Shizuku启用无障碍服务失败")
            false
        }
    }

    // 新增协程版本
    suspend fun byShizuku2(timeout: Long): Boolean {
        val context = GlobalAppContext.get()
        return try {
            val pkg = context.packageName
            val cmd = "settings put secure enabled_accessibility_services $pkg/${sAccessibilityServiceClass.name}\n" +
                    "settings put secure accessibility_enabled 1"
            Utils.exec2(cmd) // 使用协程版本的exec2
            withContext(Dispatchers.IO) {
                AccessibilityService2.waitForEnabled(timeout)
            }
        } catch (e: Exception) {
            Timber.e(e, "Shizuku启用无障碍服务失败")
            false
        }
    }
}