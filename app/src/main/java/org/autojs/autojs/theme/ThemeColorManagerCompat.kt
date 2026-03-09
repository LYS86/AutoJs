package org.autojs.autojs.theme

import com.stardust.app.GlobalAppContext
import com.stardust.theme.ThemeColor
import com.stardust.theme.ThemeColorManager
import org.autojs.autojs.PrefV2
import org.autojs.autojs.R

object ThemeColorManagerCompat {

    private var nightModeColorBackup: Int by PrefV2.int("theme_color_backup",getColorPrimary())

    private val nightModeColor: Int by lazy { GlobalAppContext.getColor(R.color.theme_color_black) }

    fun getColorPrimary(): Int {
        val color = ThemeColorManager.getColorPrimary()
        return if (color == 0) GlobalAppContext.getColor(R.color.colorPrimary) else color
    }

    fun setNightModeEnabled(enabled: Boolean) {
        when {
            enabled.not() -> ThemeColorManager.setThemeColor(nightModeColorBackup)
            else -> {
                nightModeColorBackup = getColorPrimary()
                ThemeColorManager.setThemeColor(nightModeColor)
            }
        }
    }

    fun init(defaultThemeColor: ThemeColor) {
        ThemeColorManager.setDefaultThemeColor(defaultThemeColor)
        ThemeColorManager.init(GlobalAppContext.get())
    }
}
