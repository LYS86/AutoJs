package com.stardust.auojs.inrt

import android.content.Context
import android.content.SharedPreferences

import com.stardust.app.GlobalAppContext

/**
 * Created by Stardust on 2017/12/8.
 */

object Pref {

    val preferences: SharedPreferences by lazy {
        GlobalAppContext.get().getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    }

    private fun getString(res: Int): String = GlobalAppContext.getString(res)

    private fun getBoolean(resId: Int, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(getString(resId), defaultValue)
    }

    fun shouldEnableAccessibilityServiceByRoot(): Boolean {
        return getBoolean(R.string.key_enable_accessibility_service_by_root, false)
    }

    fun shouldHideLogs(): Boolean {
        return getBoolean(R.string.key_dont_show_main_activity, false)
    }

    fun shouldStopAllScriptsWhenVolumeUp(): Boolean {
        return getBoolean(R.string.key_use_volume_control_running, true)
    }
}
