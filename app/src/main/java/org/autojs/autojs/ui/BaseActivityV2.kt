package org.autojs.autojs.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import com.google.android.material.snackbar.Snackbar
import com.stardust.theme.ThemeColorManager
import org.autojs.autojs.R
import org.autojs.autojs.theme.ThemeUtils

abstract class BaseActivityV2 : AppCompatActivity() {

    companion object {
        const val PERMISSION_REQUEST_CODE = 11186

        @JvmStatic
        fun setToolbarAsBack(activity: AppCompatActivity, id: Int, title: String) {
            val toolbar = activity.findViewById<Toolbar>(id)
            toolbar.title = title
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                toolbar.setNavigationOnClickListener { activity.finish() }
            }
        }
    }

    private val permissionRequestCallbacks = mutableMapOf<Int, (Boolean) -> Unit>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionRequestCallbacks[PERMISSION_REQUEST_CODE]?.invoke(isGranted)
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionRequestCallbacks[PERMISSION_REQUEST_CODE]?.invoke(allGranted)
    }

    override fun onStart() {
        super.onStart()
        applyStatusBarTheme()
    }

    private fun applyStatusBarTheme() {
        if (!isFullScreenLayout()) {
            ThemeColorManager.addActivityStatusBar(this)
        }
    }

    private fun isFullScreenLayout(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior == WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            (window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) != 0
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.applyDayNightMode()
    }


    fun setToolbarAsBack(title: String) {
        setToolbarAsBack(this, R.id.toolbar, title)
    }

    /**
     * 现代权限请求方法
     * @param permission 请求的权限
     * @param rationale 当需要解释权限时的说明文本
     * @param callback 权限请求结果回调 (true=已授权)
     */
    protected fun requestPermission(
        permission: String,
        rationale: String? = null,
        callback: (Boolean) -> Unit
    ) {
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            callback(true)
            return
        }

        permissionRequestCallbacks[PERMISSION_REQUEST_CODE] = callback
        if (shouldShowRequestPermissionRationale(permission) && !rationale.isNullOrBlank()) {
            Snackbar.make(
                findViewById(android.R.id.content),
                rationale,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(android.R.string.ok) {
                    requestPermissionLauncher.launch(permission)
                }
                .show()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    /**
     * 请求多个权限
     * @param permissions 权限数组
     * @param rationale 当需要解释权限时的说明文本
     * @param callback 权限请求结果回调 (true=所有权限都已授权)
     */
    protected fun requestPermissions(
        permissions: Array<String>,
        rationale: String? = null,
        callback: (Boolean) -> Unit
    ) {
        val hasAllPermissions = permissions.all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

        if (hasAllPermissions) {
            callback(true)
            return
        }

        permissionRequestCallbacks[PERMISSION_REQUEST_CODE] = callback

        val shouldShowRationale = permissions.any { shouldShowRequestPermissionRationale(it) }
        if (shouldShowRationale && !rationale.isNullOrBlank()) {
            Snackbar.make(
                findViewById(android.R.id.content),
                rationale,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(android.R.string.ok) {
                    requestMultiplePermissionsLauncher.launch(permissions)
                }
                .show()
        } else {
            requestMultiplePermissionsLauncher.launch(permissions)
        }
    }

    @CallSuper
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            permissionRequestCallbacks[requestCode]?.invoke(allGranted)
            permissionRequestCallbacks.remove(requestCode)
        }
    }

}