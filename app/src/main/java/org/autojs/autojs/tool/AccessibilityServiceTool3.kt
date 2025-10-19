package org.autojs.autojs.tool

import android.content.ActivityNotFoundException
import android.content.Context
import com.shizuku.Utils
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.core.util.ProcessShell
import com.stardust.autojs.runtime.api.AbstractShell
import com.stardust.view.accessibility.AccessibilityServiceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.autojs.autojs.PrefV2
import org.autojs.autojs.R
import timber.log.Timber
import java.util.Locale
import com.stardust.autojs.core.accessibility.AccessibilityService as AccessibilityService1
import com.stardust.view.accessibility.AccessibilityService as AccessibilityService2

object AccessibilityServiceTool3 {

    private val serviceClass = AccessibilityService1::class.java

    private const val KEY_USE_ROOT = "use_root"

    private val useRoot by PrefV2.boolean(KEY_USE_ROOT, false)

    private sealed class Shell {
        abstract suspend fun exec(cmd: String): AbstractShell.Result

        object Shizuku : Shell() {
            override suspend fun exec(cmd: String): AbstractShell.Result = Utils.execSuspend(cmd)
        }

        object Root : Shell() {
            override suspend fun exec(cmd: String): AbstractShell.Result =
                withContext(Dispatchers.IO) {
                    ProcessShell.execCommand(cmd, true)
                }
        }
    }

    private const val CMD_GET = "settings get secure enabled_accessibility_services"
    private const val CMD_PUT =
        "settings put secure enabled_accessibility_services %s\nsettings put secure accessibility_enabled 1"

    private suspend fun switchService(shell: Shell, enable: Boolean? = null): Boolean {
        val context = GlobalAppContext.get()
        val serviceName = "${context.packageName}/${serviceClass.name}"

        return try {
            shell.exec(CMD_GET).let { result ->
                result.result.trim().also { currentServices ->
                    Timber.d("当前无障碍服务: $currentServices")
                }
            }.let { currentServices ->
                val targetState = enable ?: !currentServices.contains(serviceName)
                Timber.d("目标状态: ${if (targetState) "启用" else "禁用"}")

                currentServices.splitToSet().also { servicesSet ->
                    val operation = when (targetState) {
                        true -> servicesSet.add(serviceName).also {
                            if (it) Timber.d("正在启用无障碍服务: $serviceName")
                        }

                        false -> servicesSet.remove(serviceName).also {
                            if (it) Timber.d("正在禁用无障碍服务: $serviceName")
                        }
                    }

                    if (!operation) return@let true
                }.let { servicesSet ->
                    servicesSet.joinServices().also { newServices ->
                        Timber.d("新的无障碍服务列表: $newServices")
                    }
                }.let { newServices ->
                    String.format(Locale.getDefault(), CMD_PUT, newServices).also { cmd ->
                        Timber.d("执行更新命令: $cmd")
                    }
                }.let { cmd ->
                    shell.exec(cmd).also { result ->
                        Timber.d("执行结果: $result")
                    }
                }.let {
                    isEnabled(context).also { isNowEnabled ->
                        Timber.d("操作后无障碍服务状态: $isNowEnabled")
                        isNowEnabled == targetState
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Shell切换无障碍服务失败")
            false
        }
    }

    private fun String.splitToSet() = when {
        isEmpty() || this == "null" -> mutableSetOf()
        else -> split(":").toMutableSet()
    }

    private fun Set<String>.joinServices() = when {
        isEmpty() -> "\"\""
        else -> joinToString(":")
    }

    suspend fun byShizuku(enable: Boolean? = null): Boolean {
        return switchService(Shell.Shizuku, enable)
    }

    suspend fun byRoot(enable: Boolean? = null): Boolean {
        return when {
            !useRoot -> false
            !RootTool.isRootAvailable() -> false
            else -> switchService(Shell.Root, enable)
        }
    }

    fun switchService(enable: Boolean? = null): Boolean =
        runBlocking { switchServiceSuspend(enable) }

    /**
     * 切换无障碍服务
     */
    suspend fun switchServiceSuspend(enable: Boolean? = null): Boolean {
        return byShizuku(enable) || byRoot(enable)
    }

    /**
     * 跳转到无障碍服务设置界面
     */
    @JvmStatic
    fun toSetting() {
        val context = GlobalAppContext.get()
        try {
            AccessibilityServiceUtils.goToAccessibilitySetting(context)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            GlobalAppContext.toast(
                "${context.getString(R.string.go_to_accessibility_settings)} ${context.getString(R.string.app_name)}"
            )
        }
    }


    /**
     * 无障碍服务是否已启用
     */
    fun isEnabled(context: Context): Boolean {
        return AccessibilityServiceUtils.isAccessibilityServiceEnabled(
            context, serviceClass
        )
    }

    fun waitForEnabled(time: Long = 2_000L): Boolean {
        return AccessibilityService2.waitForEnabled(time)
    }
}