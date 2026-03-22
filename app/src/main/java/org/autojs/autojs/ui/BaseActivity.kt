package org.autojs.autojs.ui

import android.graphics.PorterDuff
import android.view.Menu
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.get
import androidx.core.view.size
import com.google.android.material.snackbar.Snackbar
import com.stardust.autojs.permission.PermissionManager
import com.stardust.theme.ThemeColorManager
import org.autojs.autojs.R
import org.autojs.autojs.theme.ThemeUtils

abstract class BaseActivity : AppCompatActivity() {

    private var shouldApplyDayNightModeForOptionsMenu = true

    override fun onStart() {
        super.onStart()
        val flags = window.decorView.systemUiVisibility
        if (flags and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN == 0) {
            ThemeColorManager.addActivityStatusBar(this)
        }
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

    protected fun checkPermission(permission: String): Boolean {
        return PermissionManager.checkCompat(this, permission)
    }

    protected fun requestPermission(permission: String, callback: (Boolean) -> Unit) {
        PermissionManager.requestRuntime(this, permission, callback)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (shouldApplyDayNightModeForOptionsMenu && ThemeUtils.isDarkMode(this)) {
            repeat(menu.size) { i ->
                menu[i].icon?.apply {
                    mutate()
                    setColorFilter(
                        ContextCompat.getColor(this@BaseActivity, R.color.toolbar),
                        PorterDuff.Mode.SRC_ATOP
                    )
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
