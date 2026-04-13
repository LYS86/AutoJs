package com.stardust.autojs.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.stardust.autojs.core.pref.PrefV2

@Composable
fun SettingsCategory(
    title: String, modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    summary: String = "",
    iconRes: Int? = null,
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val hasSwitch = onCheckedChange != null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = onClick != null || hasSwitch) {
                if (hasSwitch) {
                    onCheckedChange.invoke(!checked)
                } else {
                    onClick?.invoke()
                }
            }
            .padding(vertical = 4.dp)) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = if (summary.isNotBlank()) Arrangement.Top else Arrangement.Center
        ) {
            Text(
                text = title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (hasSwitch) {
            Switch(
                checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun PrefSwitchItem(
    title: String,
    key: String,
    modifier: Modifier = Modifier,
    summary: String = "",
    iconRes: Int? = null,
    defaultValue: Boolean = false
) {
    val isPreview = LocalInspectionMode.current
    val checkedState = remember {
        mutableStateOf(
            if (isPreview) defaultValue else PrefV2.prefs.getBoolean(key, defaultValue)
        )
    }
    var checked by checkedState

    SettingsItem(
        title = title, summary = summary, iconRes = iconRes, checked = checked, onCheckedChange = { newValue ->
            checked = newValue
            if (!isPreview) {
                PrefV2.prefs.edit { putBoolean(key, newValue) }
            }
        }, modifier = modifier
    )
}

@Composable
fun PrefListItem(
    title: String,
    key: String,
    options: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    defaultValue: String = "",
    iconRes: Int? = null
) {
    val isPreview = LocalInspectionMode.current
    var selectedValue by remember {
        mutableStateOf(
            if (isPreview) defaultValue else PrefV2.prefs.getString(key, defaultValue) ?: defaultValue
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(vertical = 4.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (label, value) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = selectedValue == value,
                            onClick = {
                                selectedValue = value
                                if (!isPreview) {
                                    PrefV2.prefs.edit { putString(key, value) }
                                }
                            },
                            role = Role.RadioButton
                        )
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(end = 8.dp)
                ) {
                    RadioButton(
                        selected = selectedValue == value,
                        onClick = null
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PrefEditTextItem(
    title: String,
    key: String,
    modifier: Modifier = Modifier,
    defaultValue: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    iconRes: Int? = null
) {
    val isPreview = LocalInspectionMode.current
    var savedValue by remember {
        mutableStateOf(
            if (isPreview) defaultValue else PrefV2.prefs.getString(key, defaultValue) ?: defaultValue
        )
    }
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    fun startEditing() {
        editValue = ""
        isEditing = true
    }

    fun saveAndCollapse() {
        val valueToSave = editValue.ifBlank { defaultValue }
        if (!isPreview) {
            PrefV2.prefs.edit { putString(key, valueToSave) }
        }
        savedValue = valueToSave
        isEditing = false
        focusManager.clearFocus()
    }

    fun cancelAndCollapse() {
        editValue = savedValue
        isEditing = false
        focusManager.clearFocus()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(enabled = !isEditing) { startEditing() }
                .padding(vertical = 4.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = savedValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isEditing) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { saveAndCollapse() }
                    ),
                    singleLine = true
                )
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