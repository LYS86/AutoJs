package org.autojs.autojs.theme

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.autojs.autojs.PrefV2
import org.autojs.autojs.R
import timber.log.Timber

object ThemeUtils {

    const val MODE_FOLLOW_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    const val MODE_NIGHT = AppCompatDelegate.MODE_NIGHT_YES
    const val MODE_DAY = AppCompatDelegate.MODE_NIGHT_NO

    private var nightMode by PrefV2.int("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    /**
     * 应用主题模式
     */
    @JvmStatic
    fun applyDayNightMode() {
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /**
     * 显示主题设置对话框
     * @param context 上下文
     */
    @JvmStatic
    fun settingDialog(context: Context) {
        val options = arrayOf(
            context.getString(R.string.theme_follow_system),
            context.getString(R.string.theme_light),
            context.getString(R.string.theme_dark)
        )
        val modeValues = intArrayOf(
            MODE_FOLLOW_SYSTEM, MODE_DAY, MODE_NIGHT
        )
        val currentMode = modeValues.indexOf(AppCompatDelegate.getDefaultNightMode())
        MaterialAlertDialogBuilder(context, R.style.DialogTheme).setTitle(R.string.theme_setting)
            .setCancelable(false).setSingleChoiceItems(options, currentMode) { dialog, i ->
                dialog.dismiss()
                val mode = modeValues[i]
                nightMode = mode
                applyDayNightMode()
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    /**
     * 检查设备当前是否处于深色模式（实时系统状态）
     * @param context 上下文对象
     * @return 如果设备当前处于深色模式返回 true，否则返回 false
     */
    @JvmStatic
    fun isDarkMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES.also {
            Timber.d("isDarkMode: $it")
        }
    }
}