package org.autojs.autojs.theme

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.app.GlobalAppContext
import org.autojs.autojs.PrefV2
import org.autojs.autojs.R
import timber.log.Timber

object ThemeUtils {

    private const val MODE_FOLLOW_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    private const val MODE_NIGHT = AppCompatDelegate.MODE_NIGHT_YES
    private const val MODE_DAY = AppCompatDelegate.MODE_NIGHT_NO

    private var nightMode by PrefV2.int("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun applyDayNightMode() {
        AppCompatDelegate.setDefaultNightMode(nightMode)
//        syncThemeColor()
    }

    fun showDialog(context: Context) {
        val options = arrayOf(
            context.getString(R.string.theme_follow_system),
            context.getString(R.string.theme_light),
            context.getString(R.string.theme_dark)
        )
        val modeValues = intArrayOf(MODE_FOLLOW_SYSTEM, MODE_DAY, MODE_NIGHT)
        val currentMode = modeValues.indexOf(AppCompatDelegate.getDefaultNightMode())
        MaterialDialog.Builder(context)
            .title(R.string.theme_setting)
            .items(*options)
            .itemsCallbackSingleChoice(currentMode) { _, _, which, _ ->
                val mode = modeValues[which]
                nightMode = mode
                applyDayNightMode()
                true
            }
            .positiveText(android.R.string.ok)
            .negativeText(android.R.string.cancel)
            .show()
    }

    fun isDarkMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES.also {
            Timber.d("isDarkMode: $it")
        }
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
