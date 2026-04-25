package com.stardust.autojs.core.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import com.stardust.autojs.core.pref.PrefV2
import com.stardust.view.accessibility.AccessibilityService

class AccessibilityService : AccessibilityService() {

    companion object {
        const val KEY_STABLE_MODE = "key_stable_mode"
        const val KEY_GESTURE_OBSERVING = "key_gesture_observing"

        var isStableModeEnabled by PrefV2.boolean(KEY_STABLE_MODE, false)
        var isGestureObservingEnabled by PrefV2.boolean(KEY_GESTURE_OBSERVING, false)
    }

    override fun onServiceConnected() {
        val serviceInfo = serviceInfo
        serviceInfo.flags = when {
            isStableModeEnabled -> serviceInfo.flags and AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS.inv()
            else -> serviceInfo.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceInfo.flags = when {
                isGestureObservingEnabled -> serviceInfo.flags or AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
                else -> serviceInfo.flags and AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE.inv()
            }
        }
        setServiceInfo(serviceInfo)
        super.onServiceConnected()
    }
}