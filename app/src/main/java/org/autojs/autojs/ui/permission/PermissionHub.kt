package org.autojs.autojs.ui.permission

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

import java.util.concurrent.ConcurrentHashMap

/**
 * 运行时权限统一入口。
 * 任意 Activity / Fragment 均可通过 [request] 一次性申请权限并获取结果。
 */
object PermissionHub {

    internal const val EXTRA_PERMS    = "p"
    internal const val EXTRA_RATIONALE = "r"
    internal const val EXTRA_KEY       = "k"

    private val callbacks = ConcurrentHashMap<String, (Map<String, Boolean>) -> Unit>()

    /**
     * 在 AppCompatActivity 中申请运行时权限
     *
     * @param host      目标 Activity
     * @param perms     要申请的权限列表
     * @param rationale 向用户解释为何需要权限的提示文字，可为空
     * @param callback  授权结果回调，key 为权限名，value 为是否授予
     */
    fun request(
        host: AppCompatActivity,
        vararg perms: String,
        rationale: String? = null,
        callback: (Map<String, Boolean>) -> Unit
    ) = realRequest(host, *perms, rationale = rationale, callback = callback)

    /**
     * 在 Fragment 中申请运行时权限
     *
     * @param host      目标 Fragment
     * @param perms     要申请的权限列表
     * @param rationale 向用户解释为何需要权限的提示文字，可为空
     * @param callback  授权结果回调，key 为权限名，value 为是否授予
     */
    fun request(
        host: Fragment,
        vararg perms: String,
        rationale: String? = null,
        callback: (Map<String, Boolean>) -> Unit
    ) = realRequest(host, *perms, rationale = rationale, callback = callback)

    /**
     * 统一实现，保持私有
     */
    private fun realRequest(
        host: Any,
        vararg perms: String,
        rationale: String?,
        callback: (Map<String, Boolean>) -> Unit
    ) {
        val act = when (host) {
            is Fragment          -> host.requireActivity()
            is AppCompatActivity -> host
            else                 -> error("host must be Fragment or AppCompatActivity")
        }

        val key = "${act.hashCode()}_${perms.contentHashCode()}"
        callbacks[key] = callback

        act.startActivity(
            Intent(act, PermissionActivity::class.java).apply {
                putExtra(EXTRA_PERMS, perms)
                putExtra(EXTRA_RATIONALE, rationale)
                putExtra(EXTRA_KEY, key)
            }
        )
    }

    internal fun deliverResult(key: String, result: Map<String, Boolean>) {
        callbacks.remove(key)?.invoke(result)
    }
}