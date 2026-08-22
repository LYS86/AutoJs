package com.stardust.theme.preference

import android.preference.PreferenceFragment
import android.widget.ListView
import com.stardust.theme.ThemeColor
import com.stardust.theme.ThemeColorManager
import com.stardust.theme.ThemeColorMutable
import com.stardust.theme.internal.ScrollingViewEdgeGlowColorHelper

open class ThemeColorPreferenceFragment : PreferenceFragment(), ThemeColorMutable {

    private var themeColor = 0
    private var hasAppliedThemeColor = false

    override fun addPreferencesFromResource(resId: Int) {
        super.addPreferencesFromResource(resId)
        ThemeColorManager.add(this)
    }

    override fun setThemeColor(color: ThemeColor) {
        if (themeColor == color.colorPrimary) return
        themeColor = color.colorPrimary
        hasAppliedThemeColor = false
    }

    override fun onResume() {
        super.onResume()
        if (!hasAppliedThemeColor) {
            applyThemeColor()
        }
    }

    private fun applyThemeColor() {
        val view = view
        if (view is ListView) {
            ScrollingViewEdgeGlowColorHelper.setEdgeGlowColor(view, themeColor)
            hasAppliedThemeColor = true
        }
    }
}
