package com.stardust.autojs.core.pref

import android.content.Context
import android.content.SharedPreferences

object DefaultSharedPreferences {

    private const val NAME_SUFFIX = "_preferences"

    @JvmStatic
    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(context.packageName + NAME_SUFFIX, Context.MODE_PRIVATE)
}
