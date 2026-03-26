package com.stardust.auojs.inrt

import android.os.Bundle
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.stardust.auojs.inrt.databinding.ActivitySettingsBinding


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_setting, PreferenceFragment())
            .commit()
        
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    class PreferenceFragment : android.preference.PreferenceFragment() {

        override fun onCreate(@Nullable savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preference)
        }
    }
}
