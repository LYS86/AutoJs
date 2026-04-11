package com.stardust.autojs.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
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
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
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
            .padding(horizontal = 16.dp, vertical = 4.dp)) {
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