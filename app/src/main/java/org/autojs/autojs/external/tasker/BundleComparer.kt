package org.autojs.autojs.external.tasker

import android.os.Bundle

/** 比较两个配置 Bundle 是否按键值相同，用于判断插件配置是否真的被改动过。 */
internal object BundleComparer {

    fun areBundlesEqual(bundle: Bundle?, other: Bundle?): Boolean {
        if (bundle == null || other == null) {
            return bundle == null && other == null
        }
        if (bundle.keySet() != other.keySet()) {
            return false
        }
        return bundle.keySet().all { bundle.getString(it) == other.getString(it) }
    }
}