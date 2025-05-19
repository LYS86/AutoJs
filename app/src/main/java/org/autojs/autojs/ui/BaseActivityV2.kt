package org.autojs.autojs.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.viewbinding.ViewBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.stardust.app.GlobalAppContext
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.ui.common.DialogUtils
import org.autojs.autojs.ui.common.MessageUtils

abstract class BaseActivityV2<VB : ViewBinding> : AppCompatActivity() {

    private var mShouldApplyDayNightModeForOptionsMenu = true
    protected lateinit var binding: VB

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, isGranted) ->
            onPermissionResult(isGranted, permission)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = initView()
        setContentView(binding.root)
    }

    abstract fun initView(): VB

    protected fun Context.hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    protected fun requestPermission(permission: String) {
        requestPermissionLauncher.launch(permission)
    }

    protected fun requestPermissions(vararg permissions: String) {
        requestMultiplePermissionsLauncher.launch(permissions.toList().toTypedArray())
    }

    protected open fun onPermissionResult(isGranted: Boolean, permission: String? = null) {
        // 子类可重写
    }

    fun setupToolbar(
        toolbar: MaterialToolbar,
        title: String? = null,
        showBackButton: Boolean = true,
        backAction: (() -> Unit)? = { finish() }
    ) {
        setSupportActionBar(toolbar)
        title?.let { toolbar.title = it }

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(showBackButton)
            setDisplayShowHomeEnabled(showBackButton)
        }

        if (showBackButton) {
            toolbar.setNavigationOnClickListener { backAction?.invoke() }
        }
    }

    protected fun applyDayNightMode() {
        GlobalAppContext.post {
            if (Pref.isNightModeEnabled()) {
                setNightMode(Pref.isNightModeEnabled())
            }
        }
    }

    fun setNightMode(enabled: Boolean) {
        delegate.localNightMode = if (enabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        if (delegate.applyDayNight()) {
            recreate()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (mShouldApplyDayNightModeForOptionsMenu && Pref.isNightModeEnabled()) {
            for (i in 0 until menu.size) {
                val menuItem = menu[i]
                menuItem.icon?.let { icon ->
                    icon.mutate()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        icon.colorFilter = BlendModeColorFilter(
                            ContextCompat.getColor(this, R.color.toolbar),
                            BlendMode.SRC_ATOP
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        icon.setColorFilter(
                            ContextCompat.getColor(this, R.color.toolbar),
                            PorterDuff.Mode.SRC_ATOP
                        )
                    }
                }
            }
            mShouldApplyDayNightModeForOptionsMenu = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    fun showDialog(
        title: String? = null,
        content: String,
        positiveText: String = getString(android.R.string.ok),
        negativeText: String = getString(android.R.string.cancel),
        onPositive: (() -> Unit)? = null,
        onNegative: (() -> Unit)? = null
    ) = DialogUtils.showConfirm(
        this,
        title,
        content,
        positiveText,
        negativeText,
        onPositive,
        onNegative
    )

    fun customDialog() = DialogUtils.custom(this)

    fun showMessage(
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        forceToast: Boolean = false
    ) {
        if (forceToast) {
            MessageUtils.show(this, null, message, duration)
        } else {
            MessageUtils.show(this, binding.root, message, duration)
        }
    }

    fun showMessage(
        @StringRes resId: Int,
        duration: Int = Snackbar.LENGTH_SHORT,
        forceToast: Boolean = false
    ) {
        MessageUtils.show(this, null, resId, duration, forceToast)

    }

    fun showMessageWithAction(
        message: String,
        actionText: String,
        duration: Int = Snackbar.LENGTH_LONG,
        action: (View) -> Unit = {}
    ) {
        MessageUtils.showWithAction(
            this,
            binding.root,
            message,
            actionText,
            duration,
            action
        )
    }
}