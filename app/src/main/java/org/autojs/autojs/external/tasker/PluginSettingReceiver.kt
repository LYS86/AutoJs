package org.autojs.autojs.external.tasker

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import timber.log.Timber

/**
 * Locale/Tasker 插件「触发设置」广播基类。
 *
 * 校验通过后在后台线程执行 [firePluginSetting]，有序广播时以结果码回告宿主。
 */
abstract class PluginSettingReceiver : BroadcastReceiver() {

    final override fun onReceive(context: Context, intent: Intent) {
        if (BundleScrubber.scrub(intent)) {
            return
        }
        if (LocalePluginProtocol.ACTION_FIRE_SETTING != intent.action) {
            Timber.e("Intent action 不是 %s", LocalePluginProtocol.ACTION_FIRE_SETTING)
            return
        }
        // 拒绝隐式广播，否则本插件会响应并非发给它的请求
        if (context.packageName != intent.getPackage() &&
            ComponentName(context.packageName, javaClass.name) != intent.component
        ) {
            Timber.e("Intent 不是显式的")
            return
        }
        val bundle = intent.getBundleExtra(LocalePluginProtocol.EXTRA_BUNDLE)
        if (BundleScrubber.scrub(intent)) {
            return
        }
        if (bundle == null) {
            Timber.e("%s 缺失", LocalePluginProtocol.EXTRA_BUNDLE)
            return
        }
        if (!isBundleValid(bundle)) {
            Timber.e("%s 非法", LocalePluginProtocol.EXTRA_BUNDLE)
            return
        }
        // isOrderedBroadcast() 必须在 goAsync() 之前读取：goAsync() 会摘走 PendingResult，之后再读恒为 false
        goAsyncWithCallback(context, bundle, isOrderedBroadcast)
    }

    /** 在后台线程执行 [firePluginSetting]，让 onReceive 得以立刻返回。 */
    private fun goAsyncWithCallback(context: Context, bundle: Bundle, isOrdered: Boolean) {
        val pendingResult = goAsync()
            ?: throw AssertionError("PendingResult 为 null：goAsync() 是否已被调用过？")
        val thread = HandlerThread(javaClass.name, Process.THREAD_PRIORITY_BACKGROUND)
        thread.start()
        Handler(thread.looper).post {
            try {
                firePluginSetting(context, bundle)
                if (isOrdered) {
                    pendingResult.setResultCode(Activity.RESULT_OK)
                }
            } finally {
                pendingResult.finish()
                thread.looper.quitSafely()
            }
        }
    }

    /** 校验宿主回传的 Bundle 是否可信；在 receiver 的 Looper 上调用，实现方不得改动 [bundle]。 */
    protected abstract fun isBundleValid(bundle: Bundle): Boolean

    /** 触发插件动作；运行在后台线程，且必须在 10 秒内返回。 */
    protected abstract fun firePluginSetting(context: Context, bundle: Bundle)
}