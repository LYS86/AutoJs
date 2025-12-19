// BaseFragment.kt
package org.autojs.autojs.ui.base

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.stardust.autojs.permission.PermissionManager
import org.autojs.autojs.ui.common.MessageUtils
import org.autojs.autojs.ui.permission.PermissionHub

abstract class BaseFragment : Fragment {
    
    constructor() : super()
    
    constructor(layoutResId: Int) : super(layoutResId)

     fun hasPermission(permission: String): Boolean {
        return PermissionManager.hasPermission(requireContext(), permission)
    }

     fun requestPermission(
        permission: String,
        rationale: String? = null,
        callback: ((Boolean) -> Unit)? = null
    ) {
        PermissionHub.request(this, permission, rationale = rationale) { result ->
            callback?.invoke(result[permission] == true)
        }
    }

     fun requestPermissions(
        vararg permissions: String,
        rationale: String? = null,
        callback: ((Map<String, Boolean>) -> Unit)? = null
    ) {
        PermissionHub.request(this, *permissions, rationale = rationale, callback = callback ?: {})
    }

    fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(requireView(), message, duration).show()
    }
    fun showSnackbar(resId: Int, duration: Int = Snackbar.LENGTH_SHORT) {
       Snackbar.make(requireView(), resId, duration).show()
    }

    fun showToast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), resId, duration).show()
    }
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(requireContext(), message, duration).show()
    }

     fun showMessage(
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        forceToast: Boolean = false
    ) {
        val rootView = view
        MessageUtils.show(requireContext(), rootView, message, duration, forceToast)
    }
    fun showMessage(resId: Int, duration: Int = Snackbar.LENGTH_SHORT) {
        showMessage(getString(resId), duration)
    }
}