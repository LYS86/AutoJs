package com.stardust.autojs.core.pref

import com.stardust.app.GlobalAppContext

object Pref {
    private val preferences = DefaultSharedPreferences.get(GlobalAppContext.get())

    val isStableModeEnabled: Boolean
        get() {
            return preferences.getBoolean("key_stable_mode", false)
        }

    val isGestureObservingEnabled: Boolean
        get() {
            return preferences.getBoolean("key_gesture_observing", false)
        }
}