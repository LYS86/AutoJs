package org.autojs.autojs.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import com.google.android.material.snackbar.Snackbar
import com.stardust.theme.ThemeColorManager
import org.autojs.autojs.R
import org.autojs.autojs.theme.ThemeUtils

abstract class BaseActivityV2 : AppCompatActivity() {

    private var isActivityVisible = false

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

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionRequestCallbacks[PERMISSION_REQUEST_CODE]?.invoke(allGranted)
    }

    override fun onStart() {
        super.onStart()
        isActivityVisible = true
        applyStatusBarTheme()
    }

    override fun onStop() {
        super.onStop()
        isActivityVisible = false
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
     * 现代权限请求方法（统一使用多权限请求）
     * @param permission 请求的权限
     * @param rationale 当需要解释权限时的说明文本
     * @param callback 权限请求结果回调 (true=已授权)
     */
    fun requestPermission(
        permission: String,
        rationale: String? = null,
        callback: (Boolean) -> Unit
    ) {
        requestPermissions(arrayOf(permission), rationale, callback)
    }

    /**
     * 请求多个权限
     * @param permissions 权限数组
     * @param rationale 当需要解释权限时的说明文本
     * @param callback 权限请求结果回调 (true=所有权限都已授权)
     */
    fun requestPermissions(
        permissions: Array<String>,
        rationale: String? = null,
        callback: (Boolean) -> Unit
    ) {
        val hasAllPermissions = permissions.all { hasPermission(it) }

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
                    requestPermissionsLauncher.launch(permissions)
                }
                .show()
        } else {
            requestPermissionsLauncher.launch(permissions)
        }
    }

    fun showMessage(message: String) {
        if (isActivityVisible) {
            Snackbar.make(
                findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}