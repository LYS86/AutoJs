package org.autojs.autojs

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.stardust.app.GlobalAppContext
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object PrefV2 {

    private val context: Context
        get() = GlobalAppContext.get()

    val defaultPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val disposablePrefs: SharedPreferences by lazy {
        context.getSharedPreferences("DISPOSABLE_BOOLEAN", Context.MODE_PRIVATE)
    }

    // ================== 通用偏好设置访问 ================== //
    fun boolean(key: String, default: Boolean) = BooleanPreference(key, default)
    fun int(key: String, default: Int) = IntPreference(key, default)
    fun long(key: String, default: Long) = LongPreference(key, default)
    fun float(key: String, default: Float) = FloatPreference(key, default)
    fun string(key: String, default: String) = StringPreference(key, default)
    fun stringSet(key: String, default: Set<String>) = StringSetPreference(key, default)

    /**
     * 获取文档URL
     * 根据用户设置的文档源返回相应的URL
     * @return 文档基础URL，本地或在线
     */
    fun getDocumentationUrl(): String {
        val docSource = defaultPrefs.getString(
            context.getString(R.string.key_documentation_source), 
            null
        )
        return if (docSource == null || docSource == "Local") {
            "file:///android_asset/docs/"
        } else {
            "https://www.autojs.org/assets/autojs/docs/"
        }
    }

    /**
     * 获取脚本目录路径
     */
    fun getScriptDirPath(): String {
        val scriptDir = context.getString(R.string.default_value_script_dir_path)
        val dir = defaultPrefs.getString(
            context.getString(R.string.key_script_dir_path),
            scriptDir
        ) ?: scriptDir
        return File(Environment.getExternalStorageDirectory(), dir).path
    }

    // ================== 一次性布尔值 ================== //
    fun disposableBoolean(key: String, defaultValue: Boolean) = object : ReadOnlyProperty<Any, Boolean> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
            return disposablePrefs.getBoolean(key, defaultValue).also { currentValue ->
                if (currentValue == defaultValue) {
                    disposablePrefs.edit { putBoolean(key, !defaultValue) }
                }
            }
        }
    }

    // ================== 偏好设置委托实现 ================== //
    sealed class BasePreference<T>(
        protected val key: String,
        protected val defaultValue: T
    ) {
        protected val prefs: SharedPreferences
            get() = defaultPrefs
    }

    class BooleanPreference(key: String, defaultValue: Boolean)
        : BasePreference<Boolean>(key, defaultValue), ReadWriteProperty<Any, Boolean> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
            return prefs.getBoolean(key, defaultValue)
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
            prefs.edit { putBoolean(key, value) }
        }
    }

    class IntPreference(key: String, defaultValue: Int)
        : BasePreference<Int>(key, defaultValue), ReadWriteProperty<Any, Int> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Int {
            return prefs.getInt(key, defaultValue)
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
            prefs.edit { putInt(key, value) }
        }
    }

    class LongPreference(key: String, defaultValue: Long)
        : BasePreference<Long>(key, defaultValue), ReadWriteProperty<Any, Long> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Long {
            return prefs.getLong(key, defaultValue)
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
            prefs.edit { putLong(key, value) }
        }
    }

    class FloatPreference(key: String, defaultValue: Float)
        : BasePreference<Float>(key, defaultValue), ReadWriteProperty<Any, Float> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Float {
            return prefs.getFloat(key, defaultValue)
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Float) {
            prefs.edit { putFloat(key, value) }
        }
    }

    class StringPreference(key: String, defaultValue: String)
        : BasePreference<String>(key, defaultValue), ReadWriteProperty<Any, String> {
        override fun getValue(thisRef: Any, property: KProperty<*>): String {
            return prefs.getString(key, defaultValue) ?: defaultValue
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
            prefs.edit { putString(key, value) }
        }
    }

    class StringSetPreference(key: String, defaultValue: Set<String>)
        : BasePreference<Set<String>>(key, defaultValue), ReadWriteProperty<Any, Set<String>> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Set<String> {
            return prefs.getStringSet(key, defaultValue) ?: defaultValue
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Set<String>) {
            prefs.edit { putStringSet(key, value) }
        }
    }
}