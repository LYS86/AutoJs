package org.autojs.autojs.external.tasker

import android.content.Intent
import android.os.Bundle
import timber.log.Timber

/**
 * 清除 Intent/Bundle 中的非法 extra，抵御私有 Serializable 注入。
 *
 * 必须在读取 extras 之前调用。
 */
internal object BundleScrubber {

    /** @return true 表示发现非法内容，已清空 */
    fun scrub(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        }
        // getExtras() 返回的是副本，命中后须 replaceExtras 才能清掉 Intent 上的 extras
        val isBundleMutated = scrub(intent.extras)
        if (isBundleMutated) {
            intent.replaceExtras(Bundle())
        }
        return isBundleMutated
    }

    /** @return true 表示发现非法内容，已清空 */
    fun scrub(bundle: Bundle?): Boolean {
        if (bundle == null) {
            return false
        }
        return try {
            // Android bug 16006：extras 含私有 Serializable 时任何取值操作都会抛异常，故用一次取值探边
            bundle.containsKey(null)
            false
        } catch (e: Exception) {
            // 各系统版本抛出的异常类型不同，只能宽泛捕获
            Timber.e(e, "检测到私有 Serializable 注入，清空全部 extras")
            bundle.clear()
            true
        }
    }
}