package com.stardust.auojs.inrt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.stardust.autojs.compose.layout.TopAppBarScaffold
import com.stardust.autojs.compose.settings.PrefSwitchItem
import com.stardust.autojs.compose.settings.SettingsCategory
import com.stardust.autojs.compose.theme.AppTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
internal fun SettingsScreen(onBack: () -> Unit) {
    TopAppBarScaffold(
        title = stringResource(R.string.text_settings),
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            SettingsCategory(title = stringResource(R.string.text_script_running))
            PrefSwitchItem(
                title = stringResource(R.string.text_use_volume_to_stop_running),
                key = stringResource(R.string.key_use_volume_control_running),
                defaultValue = true
            )
            PrefSwitchItem(
                title = stringResource(R.string.text_dont_show_main_activity),
                summary = stringResource(R.string.summary_dont_show_main_activity),
                key = stringResource(R.string.key_dont_show_main_activity)
            )

            SettingsCategory(title = stringResource(R.string.text_accessibility_service))
            PrefSwitchItem(
                title = stringResource(R.string.text_enable_accessibility_service_by_root),
                summary = stringResource(R.string.summary_enable_accessibility_service_by_root),
                key = stringResource(R.string.key_enable_accessibility_service_by_root)
            )
            PrefSwitchItem(
                title = stringResource(R.string.text_stable_mode),
                summary = stringResource(R.string.summary_stable_mode),
                key = stringResource(R.string.key_stable_mode)
            )
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(onBack = {})
    }
}
