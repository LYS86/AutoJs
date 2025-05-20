package org.autojs.autojs.ui.base

import android.content.pm.PackageManager
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.autojs.autojs.ui.common.MessageUtils

abstract class BaseFragment : Fragment() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted, requestedPermission)
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (permission, isGranted) ->
            onPermissionResult(isGranted, permission)
        }
    }

    private var requestedPermission: String? = null
    private var permissionCallback: ((Boolean) -> Unit)? = null

    protected fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    protected fun requestPermission(
        permission: String,
        callback: ((Boolean) -> Unit)? = null
    ) {
        requestedPermission = permission
        permissionCallback = callback
        requestPermissionLauncher.launch(permission)
    }

    protected fun requestPermissions(
        vararg permissions: String,
        callback: ((Map<String, Boolean>) -> Unit)? = null
    ) {
        requestMultiplePermissionsLauncher.launch(permissions.toList().toTypedArray())
    }

    protected open fun onPermissionResult(isGranted: Boolean, permission: String?) {
        permissionCallback?.invoke(isGranted)
    }

    protected fun showMessage(
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        forceToast: Boolean = false
    ) {
        val rootView = view
        MessageUtils.show(requireContext(), rootView, message, duration, forceToast)
    }

    protected fun showMessage(
        @StringRes resId: Int,
        duration: Int = Snackbar.LENGTH_SHORT,
        forceToast: Boolean = false
    ) {
        val rootView = view
        MessageUtils.show(requireContext(), rootView, resId, duration, forceToast)
    }

    protected fun showMessageWithAction(
        message: String,
        actionText: String,
        duration: Int = Snackbar.LENGTH_LONG,
        action: (View) -> Unit = {}
    ) {
        val rootView = view
        MessageUtils.showWithAction(requireContext(), rootView, message, actionText, duration, action)
    }
}