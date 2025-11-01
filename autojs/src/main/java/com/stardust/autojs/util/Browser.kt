package com.stardust.autojs.util

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.stardust.autojs.R

object Browser {

    fun test(context: Context) {
        openUrl(context, "https://github.com/LYS86/AutoJs/issues")
    }

    @JvmStatic
    fun openUrl(context: Context, url: String) {
        CustomTabsIntent.Builder().setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
            .setUrlBarHidingEnabled(true)
            .build()
            .launchUrl(context, url.toUri())
    }

}