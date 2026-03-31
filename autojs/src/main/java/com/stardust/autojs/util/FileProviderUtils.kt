package com.stardust.autojs.util

import android.content.Context

object FileProviderUtils {

    @JvmStatic
    fun getAuthority(context: Context): String {
        return "${context.packageName}.fileprovider"
    }
}
