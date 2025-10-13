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
import org.autojs.autojs.ui.permission.PermissionHub
import timber.log.Timber

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


    fun setToolbarAsBack(title: String) {
        setToolbarAsBack(this, R.id.toolbar, title)
    }

    /**
     * 单权限
     */
    fun requestPermission(
        permission: String,
        rationale: String? = null,
        callback: (Boolean) -> Unit
    ) {
        requestPermissions(arrayOf(permission), rationale) { result ->
            callback(result[permission] == true)
        }
    }

    /**
     * 多权限
     */
    fun requestPermissions(
        permissions: Array<String>,
        rationale: String? = null,
        callback: (Map<String, Boolean>) -> Unit
    ) {
        val current = permissions.associateWith { hasPermission(it) }
        if (current.values.all { it }) {
            callback(current)
            return
        }

        PermissionHub.request(
            host = this,
            perms = permissions,
            rationale = rationale
        ) { callback(it) }
    }

    fun showMessage(message: String) {
        Timber.d(message)
        if (isActivityVisible) {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun  showMessage(resId: Int) {
        if (isActivityVisible) {
            Snackbar.make(findViewById(android.R.id.content), resId, Snackbar.LENGTH_SHORT).show()
        }else {
            Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
        }

    }

    fun hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}