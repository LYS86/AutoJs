package org.autojs.autojs.tool

import android.content.Intent
import android.os.Build
import android.os.Looper
import com.stardust.app.GlobalAppContext
import com.stardust.view.accessibility.AccessibilityService
import timber.log.Timber
import java.lang.Thread.UncaughtExceptionHandler

/**
 * 崩溃处理器 V2 - 仅使用 Timber 记录崩溃信息
 * 移除了 Bugly 依赖，简化崩溃处理逻辑
 */
class CrashHandlerV2(
    private val errorReportClass: Class<*>
) : UncaughtExceptionHandler {

    private var systemHandler: UncaughtExceptionHandler? = null
    private var crashCount = 0
    private var firstCrashMillis = 0L

    init {
        // 保存系统默认的异常处理器（在 CrashHandler 设置之前）
        systemHandler = Thread.getDefaultUncaughtExceptionHandler()
    }

    /**
     * 处理未捕获的异常
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Timber.e(throwable, "未捕获异常 - 线程: ${thread.name}")

        // 如果不是主线程，直接记录并返回
        if (thread != Looper.getMainLooper().thread) {
            Timber.w("子线程异常，仅记录日志")
            return
        }

        // 主线程异常，进行清理和报告
        handleMainThreadCrash(thread, throwable)
    }

    /**
     * 处理主线程崩溃
     */
    private fun handleMainThreadCrash(thread: Thread, throwable: Throwable) {
        try {
            // 清理资源
            cleanupResources()
            Timber.d("已清理资源")

            // 检查是否频繁崩溃
            if (isCrashingTooFrequently()) {
                Timber.w("应用频繁崩溃，跳过错误报告界面")
                return
            }

            // 启动错误报告界面
            startErrorReportActivity(throwable)
        } catch (e: Exception) {
            Timber.e(e, "处理崩溃时发生异常")
        }
        // 注意：不调用 systemHandler，让主处理器（CrashHandler）处理
    }

    /**
     * 清理资源
     */
    private fun cleanupResources() {
        // 禁用无障碍
        AccessibilityService.disable()
    }



    /**
     * 检查是否频繁崩溃
     */
    private fun isCrashingTooFrequently(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // 如果距离上次崩溃超过3秒，重置计数
        if (currentTime - firstCrashMillis > 3000) {
            resetCrashCount()
            return false
        }
        
        crashCount++
        Timber.w("崩溃计数: $crashCount")
        
        return crashCount >= 5
    }

    /**
     * 重置崩溃计数
     */
    private fun resetCrashCount() {
        firstCrashMillis = System.currentTimeMillis()
        crashCount = 0
        Timber.d("重置崩溃计数")
    }

    /**
     * 启动错误报告界面
     */
    private fun startErrorReportActivity(throwable: Throwable) {
        try {
            val intent = Intent().apply {
                setClass(GlobalAppContext.get(), errorReportClass)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("message", throwable.javaClass.simpleName + ": " + throwable.message)
                putExtra("error", getStackTraceString(throwable))
            }
            
            GlobalAppContext.get().startActivity(intent)
            Timber.i("已启动错误报告界面")
        } catch (e: Exception) {
            Timber.e(e, "启动错误报告界面失败")
        }
    }

    /**
     * 获取异常堆栈信息
     */
    private fun getStackTraceString(throwable: Throwable): String {
        return try {
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            throwable.printStackTrace(pw)
            sw.toString()
        } catch (e: Exception) {
            "无法获取堆栈信息: ${e.message}"
        }
    }

    companion object {
        @Volatile
        private var instance: CrashHandlerV2? = null

        /**
         * 初始化 CrashHandlerV2 监听器
         */
        fun init(errorReportClass: Class<*>) {
            val crashHandler = CrashHandlerV2(errorReportClass)
            instance = crashHandler
            Timber.i("CrashHandlerV2 已初始化为监听器")
        }

        /**
         * 触发崩溃处理（供其他处理器调用）
         */
        @JvmStatic
        fun handleCrash(thread: Thread, throwable: Throwable) {
            instance?.uncaughtException(thread, throwable)
        }
    }
}