package com.stardust.autojs.util

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import timber.log.Timber

object Browser {

    @JvmStatic
    fun test(context: Context) {
        openUrl(context, "https://cn.bing.com/")
    }

    @JvmStatic
    fun openUrl(context: Context, url: String) {
        try {
            CustomTabsIntent.Builder().setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
                .setUrlBarHidingEnabled(true).build().launchUrl(context, url.toUri())
        } catch (e: Exception) {
            Timber.e(e)
            openWithBrowser(context, url)
        }
    }

    private fun openWithBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

}