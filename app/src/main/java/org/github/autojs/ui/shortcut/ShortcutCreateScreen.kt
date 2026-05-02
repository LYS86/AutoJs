package org.github.autojs.ui.shortcut

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.autojs.autojs.R

private val TOUCH_SIZE = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutCreateScreen(
    name: String,
    onNameChange: (String) -> Unit,
    isDynamic: Boolean,
    onDynamicChange: (Boolean) -> Unit,
    iconBitmap: Bitmap?,
    isDefaultIcon: Boolean,
    onSelectIcon: () -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.text_send_shortcut),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isDefaultIcon || iconBitmap == null) {
                    IconButton(
                        onClick = onSelectIcon,
                        modifier = Modifier.size(TOUCH_SIZE)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.text_select_icon),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Image(
                        bitmap = iconBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(TOUCH_SIZE)
                            .clip(CircleShape)
                            .clickable(onClick = onSelectIcon)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.text_name)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = isDynamic,
                            onClick = { onDynamicChange(true) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 12.dp)
                ) {
                    RadioButton(selected = isDynamic, onClick = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.text_dynamic_shortcut))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = !isDynamic,
                            onClick = { onDynamicChange(false) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 12.dp)
                ) {
                    RadioButton(selected = !isDynamic, onClick = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.text_pinned_shortcut))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onCreate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TOUCH_SIZE)
            ) {
                Text(stringResource(R.string.ok))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
