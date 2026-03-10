package org.autojs.autojs.ui

import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.material.snackbar.Snackbar
import com.stardust.app.GlobalAppContext
import com.stardust.theme.ThemeColorManager
import org.autojs.autojs.R
import androidx.core.view.size
import androidx.core.view.get
import org.autojs.autojs.theme.ThemeUtils

abstract class BaseActivity : AppCompatActivity() {

    private var shouldApplyDayNightModeForOptionsMenu = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        ThemeUtils.applyDayNightMode()
    }

    override fun onStart() {
        super.onStart()
        val flags = window.decorView.systemUiVisibility
        if (flags and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN == 0) {
            ThemeColorManager.addActivityStatusBar(this)
        }
    }

    protected fun checkPermission(vararg permissions: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val requestPermissions = getRequestPermissions(permissions)
            if (requestPermissions.isNotEmpty()) {
                requestPermissions(requestPermissions, PERMISSION_REQUEST_CODE)
                false
            } else {
                true
            }
        } else {
            val grantResults = IntArray(permissions.size)
            onRequestPermissionsResult(PERMISSION_REQUEST_CODE, permissions as Array<String>, grantResults)
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getRequestPermissions(permissions: Array<out String>): Array<String> {
        return permissions.filter { checkSelfPermission(it) == PackageManager.PERMISSION_DENIED }.toTypedArray()
    }

    fun setToolbarAsBack(title: String) {
        setToolbarAsBack(this, R.id.toolbar, title)
    }

    protected fun setToolbar(@StringRes id: Int): Toolbar {
        val toolbar = WindowCompat.requireViewById<Toolbar>(window, R.id.toolbar)
        toolbar.title = getString(id)
        setSupportActionBar(toolbar)
        return toolbar
    }

    @CallSuper
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (shouldApplyDayNightModeForOptionsMenu && ThemeUtils.isDarkMode(this)) {
            repeat(menu.size) { i ->
                menu[i].icon?.apply {
                    mutate()
                    setColorFilter(ContextCompat.getColor(this@BaseActivity, R.color.toolbar), PorterDuff.Mode.SRC_ATOP)
                }
            }
            shouldApplyDayNightModeForOptionsMenu = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    protected fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        WindowCompat.requireViewById<View>(window, android.R.id.content).let {
            Snackbar.make(it, message, duration).show()
        }
    }

    protected fun showSnackbar(@StringRes messageRes: Int, duration: Int = Snackbar.LENGTH_SHORT) {
        WindowCompat.requireViewById<View>(window, android.R.id.content).let {
            Snackbar.make(it, messageRes, duration).show()
        }
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 11186

        fun setToolbarAsBack(activity: AppCompatActivity, id: Int, title: String) {
            val toolbar = WindowCompat.requireViewById<Toolbar>(activity.window, id)
            toolbar.title = title
            activity.setSupportActionBar(toolbar)
            activity.supportActionBar?.let {
                toolbar.setNavigationOnClickListener { activity.finish() }
                it.setDisplayHomeAsUpEnabled(true)
            }
        }
    }
}
