package com.stardust.auojs.inrt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.stardust.auojs.inrt.databinding.ActivitySettingsBinding
import com.stardust.auojs.inrt.databinding.ItemCategoryBinding
import com.stardust.auojs.inrt.databinding.ItemPreferenceSwitchBinding


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.toolbar.setNavigationOnClickListener { finish() }
        loadSettings()
    }

    private fun loadSettings() {
        val container = binding.settingsContainer
        val settingsViews = listOf(
            settingsCategory(getString(R.string.text_script_running)),
            settingsItem(
                title = getString(R.string.text_use_volume_to_stop_running),
                key = getString(R.string.key_use_volume_control_running),
                defaultValue = true
            ),
            settingsItem(
                title = getString(R.string.text_dont_show_main_activity),
                summary = getString(R.string.summary_dont_show_main_activity),
                key = getString(R.string.key_dont_show_main_activity)
            ),
            settingsCategory(getString(R.string.text_accessibility_service)),
            settingsItem(
                title = getString(R.string.text_enable_accessibility_service_by_root),
                summary = getString(R.string.summary_enable_accessibility_service_by_root),
                key = getString(R.string.key_enable_accessibility_service_by_root)
            ),
            settingsItem(
                title = getString(R.string.text_stable_mode),
                summary = getString(R.string.summary_stable_mode),
                key = getString(R.string.key_stable_mode)
            )
        )

        settingsViews.forEach { container.addView(it) }
    }

    fun settingsItem(
        title: CharSequence,
        summary: CharSequence = "",
        iconRes: Int? = null,
        key: String,
        defaultValue: Boolean = false
    ): View {
        val itemBinding = ItemPreferenceSwitchBinding.inflate(LayoutInflater.from(this))
        iconRes?.let { itemBinding.icon.setImageResource(it) }
        itemBinding.title.text = title
        if (summary.isNotBlank()) {
            itemBinding.summary.text = summary
            itemBinding.summary.visibility = View.VISIBLE
        } else {
            itemBinding.summary.visibility = View.GONE
        }
        val currentValue = Pref.preferences.getBoolean(key, defaultValue)
        itemBinding.switchWidget.isChecked = currentValue
        itemBinding.root.setOnClickListener {
            val newValue = !itemBinding.switchWidget.isChecked
            itemBinding.switchWidget.isChecked = newValue
            Pref.preferences.edit().putBoolean(key, newValue).apply()
        }
        return itemBinding.root
    }

    fun settingsCategory(title: CharSequence): View {
        val categoryBinding = ItemCategoryBinding.inflate(LayoutInflater.from(this))
        categoryBinding.title.text = title
        return categoryBinding.root
    }
}
