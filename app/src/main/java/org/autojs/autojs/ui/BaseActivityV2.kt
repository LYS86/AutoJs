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
        @JvmStatic
        fun setToolbarAsBack(activity: AppCompatActivity, id: Int, title: String) {
            val toolbar = activity.findViewById<Toolbar>(id)
            toolbar.title = title
            activity.setSupportActionBar(toolbar)
            val isRoot = activity.isTaskRoot
            activity.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(!isRoot)
                if (!isRoot) {
                    toolbar.setNavigationOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
                }
            }
        }
    }

    private var permissionRequestCallback: ((Map<String, Boolean>) -> Unit)? = null

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionRequestCallback?.invoke(result)
        permissionRequestCallback = null
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

    fun requestPermission(
        permission: String,
        rationale: String? = null,
        callback: (Boolean) -> Unit
    ) {
        requestPermissions(arrayOf(permission), rationale) { result ->
            callback(result[permission] == true)
        }
    }

    fun requestPermissions(
        permissions: Array<String>,
        rationale: String? = null,
        callback: (Map<String, Boolean>) -> Unit
    ) {
        val currentStatus = permissions.associateWith { hasPermission(it) }
        if (currentStatus.values.all { it }) {
            callback(currentStatus)
            return
        }

        permissionRequestCallback = callback

        if (!rationale.isNullOrBlank()) {
            Snackbar.make(findViewById(android.R.id.content), rationale, Snackbar.LENGTH_INDEFINITE)
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
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}