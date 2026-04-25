package org.autojs.autojs.ui.compose.settings

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.stardust.autojs.compose.layout.TopAppBarScaffold
import com.stardust.autojs.compose.settings.PrefEditTextItem
import com.stardust.autojs.compose.settings.PrefListItem
import com.stardust.autojs.compose.settings.PrefSwitchItem
import com.stardust.autojs.compose.settings.SettingsCategory
import com.stardust.autojs.compose.settings.SettingsItem
import com.stardust.autojs.core.accessibility.AccessibilityService
import com.stardust.autojs.util.AccessibilityServiceUtils
import com.stardust.autojs.core.pref.PrefV2
import com.stardust.autojs.util.Browser
import de.psdev.licensesdialog.LicenseResolver
import de.psdev.licensesdialog.LicensesDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.R
import org.autojs.autojs.model.explorer.Explorers
import timber.log.Timber
import java.io.File

@Composable
internal fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val myGithub = stringResource(R.string.my_github)

    TopAppBarScaffold(
        title = stringResource(R.string.text_setting),
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            SettingsCategory(title = stringResource(R.string.text_script_record))
            PrefSwitchItem(
                title = stringResource(R.string.text_use_volume_control_record),
                summary = stringResource(R.string.summary_use_volume_control_record),
                key = stringResource(R.string.key_use_volume_control_record)
            )
            PrefSwitchItem(
                title = stringResource(R.string.text_record_msg),
                key = stringResource(R.string.key_record_toast),
                defaultValue = true
            )
            PrefListItem(
                title = stringResource(R.string.text_root_record_out_file_type),
                key = stringResource(R.string.key_root_record_out_file_type),
                options = listOf(
                    stringResource(R.string.text_js_file) to "js",
                    stringResource(R.string.text_binary_file) to "binary"
                ),
                defaultValue = stringResource(R.string.text_binary)
            )

            SettingsCategory(title = stringResource(R.string.text_script_running))
            PrefSwitchItem(
                title = stringResource(R.string.text_use_volume_to_stop_running),
                key = stringResource(R.string.key_use_volume_control_running)
            )
            PrefSwitchItem(
                title = stringResource(R.string.text_guard_mode),
                summary = stringResource(R.string.summary_guard_mode),
                key = stringResource(R.string.key_guard_mode)
            )

            SettingsCategory(title = stringResource(R.string.text_edit))
            PrefEditTextItem(
                title = stringResource(R.string.text_max_length_for_code_completion),
                key = stringResource(R.string.key_max_length_for_code_completion),
                defaultValue = "2000",
                keyboardType = KeyboardType.Number
            )

            SettingsCategory(title = stringResource(R.string.text_accessibility_service))
            PrefSwitchItem(
                title = stringResource(R.string.text_auto_enable_service),
                summary = stringResource(R.string.summary_auto_enable_service),
                key = AccessibilityServiceUtils.KEY
            )
            PrefSwitchItem(
                title = stringResource(R.string.text_stable_mode),
                summary = stringResource(R.string.summary_stable_mode),
                key = AccessibilityService.KEY_STABLE_MODE
            )

            SettingsCategory(title = stringResource(R.string.text_appearance))
            SettingsItem(
                title = stringResource(R.string.text_theme_color),
                onClick = { SettingsActivity.selectThemeColor(context) }
            )

            SettingsCategory(title = stringResource(R.string.text_others))
            PrefListItem(
                title = stringResource(R.string.text_documentation_source),
                key = stringResource(R.string.key_documentation_source),
                options = listOf(
                    stringResource(R.string.text_local_document) to "Local",
                    stringResource(R.string.text_online_document) to "Online"
                )
            )
            ScriptDirPathItem(
                title = stringResource(R.string.text_change_script_dir),
                key = stringResource(R.string.key_script_dir_path),
                defaultValue = stringResource(R.string.default_value_script_dir_path)
            )

            SettingsCategory(title = stringResource(R.string.text_about))
            SettingsItem(
                title = stringResource(R.string.text_issue_report),
                onClick = { Browser.openUrl(context, "$myGithub/issues") }
            )
            SettingsItem(
                title = stringResource(R.string.text_about_me_and_repo),
                onClick = { Browser.openUrl(context, myGithub) }
            )
            SettingsItem(
                title = stringResource(R.string.text_licenses),
                onClick = { showLicenseDialog(context) }
            )
        }
    }
}

private fun showLicenseDialog(context: android.content.Context) {
    LicenseResolver.registerLicense(SettingsActivity.MozillaPublicLicense20.instance)
    if (context is androidx.activity.ComponentActivity) {
        LicensesDialog.Builder(context)
            .setNotices(R.raw.licenses)
            .setIncludeOwnLicense(true)
            .build()
            .show()
    }
}

@Composable
private fun ScriptDirPathItem(
    title: String,
    key: String,
    defaultValue: String,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val extStorage = Environment.getExternalStorageDirectory().path
    val savedPath = PrefV2.prefs.getString(key, defaultValue) ?: defaultValue
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf(ScriptDirAction.NONE) }

    fun startEditing() {
        editValue = ""
        selectedAction = ScriptDirAction.NONE
        isEditing = true
    }

    fun saveAndCollapse() {
        val newPath = editValue.ifBlank { defaultValue }
        val oldPath = PrefV2.prefs.getString(key, defaultValue) ?: defaultValue
        PrefV2.prefs.edit { putString(key, newPath) }
        isEditing = false
        focusManager.clearFocus()

        if (newPath != oldPath && selectedAction != ScriptDirAction.NONE) {
            val srcDir = File(extStorage, oldPath)
            val dstDir = File(extStorage, newPath)
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        if (selectedAction == ScriptDirAction.COPY) {
                            srcDir.copyRecursively(dstDir, overwrite = true)
                        } else {
                            srcDir.copyRecursively(dstDir, overwrite = true)
                            srcDir.deleteRecursively()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "File operation failed")
                    }
                }
                Explorers.workspace().refreshAll()
            }
        }
    }

    fun cancelAndCollapse() {
        editValue = ""
        isEditing = false
        focusManager.clearFocus()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(vertical = 4.dp)
                .then(
                    if (!isEditing) Modifier.clickable { startEditing() } else Modifier
                )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = savedPath,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isEditing) {
            OutlinedTextField(
                value = editValue,
                onValueChange = { editValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { saveAndCollapse() }),
                singleLine = true
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .padding(top = 4.dp)
            ) {
                ScriptDirAction.entries.forEach { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedAction == action,
                                onClick = { selectedAction = action },
                                role = Role.RadioButton
                            )
                            .height(48.dp)
                    ) {
                        RadioButton(
                            selected = selectedAction == action,
                            onClick = null
                        )
                        Text(
                            text = stringResource(action.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                TextButton(onClick = { saveAndCollapse() }) {
                    Text(text = stringResource(android.R.string.ok))
                }
                TextButton(onClick = { cancelAndCollapse() }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}

private enum class ScriptDirAction(val labelRes: Int) {
    NONE(R.string.text_no_action),
    COPY(R.string.text_copy_files),
    MOVE(R.string.text_move_files)
}
