package org.autojs.autojs

import android.content.Context
import android.content.SharedPreferences
import com.stardust.app.GlobalAppContext
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object PrefV2 {
    @JvmField
    var prefs: SharedPreferences = GlobalAppContext.get().getSharedPreferences("user_settings", Context.MODE_PRIVATE)

    fun string(key: String, defaultValue: String = ""): ReadWriteProperty<Any?, String> =
        PreferenceDelegate(key, defaultValue,
            { k, v -> prefs.getString(k, v)!! },
            { k, v -> prefs.edit().putString(k, v) }
        )

    fun int(key: String, defaultValue: Int = 0): ReadWriteProperty<Any?, Int> =
        PreferenceDelegate(key, defaultValue,
            { k, v -> prefs.getInt(k, v) },
            { k, v -> prefs.edit().putInt(k, v) }
        )

    fun boolean(key: String, defaultValue: Boolean = false): ReadWriteProperty<Any?, Boolean> =
        PreferenceDelegate(key, defaultValue,
            { k, v -> prefs.getBoolean(k, v) },
            { k, v -> prefs.edit().putBoolean(k, v) }
        )

    fun long(key: String, defaultValue: Long = 0L): ReadWriteProperty<Any?, Long> =
        PreferenceDelegate(key, defaultValue,
            { k, v -> prefs.getLong(k, v) },
            { k, v -> prefs.edit().putLong(k, v) }
        )

    fun float(key: String, defaultValue: Float = 0f): ReadWriteProperty<Any?, Float> =
        PreferenceDelegate(key, defaultValue,
            { k, v -> prefs.getFloat(k, v) },
            { k, v -> prefs.edit().putFloat(k, v) }
        )
}

private class PreferenceDelegate<T>(
    private val key: String,
    private val defaultValue: T,
    private val getter: (String, T) -> T,
    private val setter: (String, T) -> SharedPreferences.Editor
) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getter(key, defaultValue)
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setter(key, value).apply()
    }
}
