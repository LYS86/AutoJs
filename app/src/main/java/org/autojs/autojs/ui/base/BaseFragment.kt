package org.autojs.autojs.ui.base

import android.widget.Toast
import androidx.fragment.app.Fragment
import org.autojs.autojs.ui.BaseActivityV2

abstract class BaseFragment : Fragment() {

    private fun getBaseActivity(): BaseActivityV2? {
        return activity as? BaseActivityV2
    }

    fun requestPermission(
        permission: String,
        rationale: String? = null,
        callback: (Boolean) -> Unit
    ) {
        getBaseActivity()?.requestPermission(permission, rationale, callback)
            ?: callback(false) // 或者抛出异常
    }

    fun requestPermissions(
        permissions: Array<String>,
        rationale: String? = null,
        callback: (Map<String, Boolean>) -> Unit
    ) {
        getBaseActivity()?.requestPermissions(permissions, rationale, callback)
            ?: callback(emptyMap())
    }

    fun showMessage(message: String) {
        getBaseActivity()?.showMessage(message)
    }
}