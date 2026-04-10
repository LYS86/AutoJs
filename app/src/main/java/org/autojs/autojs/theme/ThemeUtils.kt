package org.autojs.autojs.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.core.pref.PrefV2
import org.autojs.autojs.R
import org.autojs.autojs.theme.dialog.MaterialAlertDialog

object ThemeUtils {

    private const val MODE_FOLLOW_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    private const val MODE_NIGHT = AppCompatDelegate.MODE_NIGHT_YES
    private const val MODE_DAY = AppCompatDelegate.MODE_NIGHT_NO

    private var nightMode by PrefV2.int("night_mode", MODE_FOLLOW_SYSTEM)
    private val uiModeManager by lazy {
        (GlobalAppContext.get().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager)
    }

    fun showCompat(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            showUiModeThemeDialog(context)
        } else {
            showAppCompatThemeDialog(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun showUiModeThemeDialog(context: Context) {
        val options = getThemeOptions(context)
        MaterialAlertDialog(context).setTitle(R.string.theme_setting)
            .setItems(options) { dialog, which ->
                val mode = when (which) {
                    0 -> UiModeManager.MODE_NIGHT_AUTO
                    1 -> UiModeManager.MODE_NIGHT_NO
                    else -> UiModeManager.MODE_NIGHT_YES
                }
                uiModeManager.setApplicationNightMode(mode)
                dialog.dismiss()
            }.setCancelable(false).show()
    }

    fun showAppCompatThemeDialog(context: Context) {
        val options = getThemeOptions(context)
        MaterialAlertDialog(context).setTitle(R.string.theme_setting)
            .setItems(options) { dialog, which ->
                val mode = when (which) {
                    0 -> MODE_FOLLOW_SYSTEM
                    1 -> MODE_DAY
                    else -> MODE_NIGHT
                }
                AppCompatDelegate.setDefaultNightMode(mode)
                dialog.dismiss()
            }.setCancelable(false).show()

    }

    private fun getThemeOptions(context: Context) = arrayOf(
        context.getString(R.string.theme_follow_system),
        context.getString(R.string.theme_light),
        context.getString(R.string.theme_dark),
    )

    fun isDarkMode(context: Context): Boolean {
        val currentNightMode =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun syncThemeColor() {
        val isDark = when (nightMode) {
            MODE_NIGHT -> true
            MODE_DAY -> false
            else -> isDarkMode(GlobalAppContext.get())
        }
        ThemeColorManagerCompat.setNightModeEnabled(isDark)
    }
}
