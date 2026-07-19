package com.stardust.autojs.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent

object Browser {

    private const val GITHUB_BASE_URL = "https://github.com/LYS86/AutoJs"

    @JvmStatic
    fun openGithub(context: Context) {
        openUrl(context, GITHUB_BASE_URL)
    }

    @JvmStatic
    fun openGithubIssue(context: Context) {
        openUrl(context, GITHUB_BASE_URL + "/issues")
    }

    @JvmStatic
    fun openUrl(context: Context, url: String) {
        try {
            CustomTabsIntent.Builder().setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
                .setUrlBarHidingEnabled(true).build().launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            Log.w("Browser", e.message)
            openWithBrowser(context, url)
        }
    }

    private fun openWithBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }

}