package org.github.autojs.ui.shortcut

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.autojs.autojs.R

data class AppItem(
    val packageName: String, val label: String, val iconBitmap: Bitmap
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutIconSelectScreen(
    appList: List<AppItem>,
    onAppSelected: (String) -> Unit,
    onPickImage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.text_select_icon)) }, navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }, actions = {
            IconButton(onClick = onPickImage) {
                Icon(imageVector = Icons.Rounded.PhotoLibrary,
                     contentDescription = stringResource(R.string.text_select_image))
            }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary))
    }, modifier = modifier) { padding ->
        BoxWithConstraints(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 8.dp)) {
            val cellSize = 64.dp
            val columns = (maxWidth / cellSize).toInt().coerceIn(3, 8)
            LazyVerticalGrid(columns = GridCells.Fixed(columns),
                             modifier = Modifier
                                 .fillMaxSize()) {
                items(appList, key = { it.packageName }) { app ->
                    AppIconCell(app = app, onClick = { onAppSelected(app.packageName) })
                }
            }
        }
    }
}

@Composable
private fun AppIconCell(app: AppItem, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier
        .padding(8.dp)
        .clickable(onClick = onClick)) {
        Image(bitmap = app.iconBitmap.asImageBitmap(),
              contentDescription = app.label,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxWidth().aspectRatio(1f))
    }
}
