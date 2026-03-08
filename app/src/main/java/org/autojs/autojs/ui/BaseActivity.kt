package org.autojs.autojs.ui

import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.stardust.app.GlobalAppContext
import com.stardust.theme.ThemeColorManager
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import androidx.core.view.size
import androidx.core.view.get

abstract class BaseActivity : AppCompatActivity() {

    private var shouldApplyDayNightModeForOptionsMenu = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun applyDayNightMode() {
        GlobalAppContext.post {
            if (Pref.isNightModeEnabled()) {
                setNightModeEnabled(Pref.isNightModeEnabled())
            }
        }
    }

    fun setNightModeEnabled(enabled: Boolean) {
        val mode = if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        delegate.setLocalNightMode(mode)
        if (delegate.applyDayNight()) {
            recreate()
        }
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

    @CallSuper
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (shouldApplyDayNightModeForOptionsMenu && Pref.isNightModeEnabled()) {
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
