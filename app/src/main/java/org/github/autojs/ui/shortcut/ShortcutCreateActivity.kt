package org.github.autojs.ui.shortcut

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.stardust.autojs.compose.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.R
import org.autojs.autojs.external.shortcut.ShortcutManager
import org.autojs.autojs.model.script.ScriptFile
import org.github.autojs.shortcut.EXTRA_PACKAGE_NAME
import timber.log.Timber

class ShortcutCreateActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FILE = "file"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scriptFile = IntentCompat.getSerializableExtra(intent, EXTRA_FILE, ScriptFile::class.java)!!
        setContent {
            AppTheme {
                ShortcutCreateContent(scriptFile, ::finish)
            }
        }
    }
}

@Composable
private fun ShortcutCreateContent(scriptFile: ScriptFile, finish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var iconBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isDefaultIcon by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf(scriptFile.simplifiedName) }
    var isDynamic by remember { mutableStateOf(true) }

    val iconSelectLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
            val data = result.data ?: return@rememberLauncherForActivityResult
            val packageName = data.getStringExtra(EXTRA_PACKAGE_NAME)
            if (packageName != null) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(packageName)
                    iconBitmap = drawable.toBitmap(width = drawable.intrinsicWidth, height = drawable.intrinsicHeight)
                    isDefaultIcon = false
                } catch (e: PackageManager.NameNotFoundException) {
                    Timber.w(e)
                }
                return@rememberLauncherForActivityResult
            }
            val uri = data.data ?: return@rememberLauncherForActivityResult
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                }
                if (bitmap != null) {
                    iconBitmap = bitmap
                    isDefaultIcon = false
                }
            }
        }

    ShortcutCreateScreen(name = name,
                         onNameChange = { name = it },
                         isDynamic = isDynamic,
                         onDynamicChange = { isDynamic = it },
                         iconBitmap = iconBitmap,
                         isDefaultIcon = isDefaultIcon,
                         onSelectIcon = {
                             iconSelectLauncher.launch(Intent(context, ShortcutIconSelectActivity::class.java))
                         },
                         onCreate = {
                             val icon = if (isDefaultIcon) {
                                 IconCompat.createWithResource(context, R.drawable.ic_node_js_black)
                             } else {
                                 IconCompat.createWithBitmap(iconBitmap!!)
                             }
                             if (isDynamic) {
                                 ShortcutManager.createDynamicShortcut(context,
                                                                       name,
                                                                       scriptFile.path,
                                                                       icon,
                                                                       scriptFile.path)
                             } else {
                                 ShortcutManager.createPinnedShortcut(context,
                                                                      name,
                                                                      scriptFile.path,
                                                                      icon,
                                                                      scriptFile.path)
                             }
                             finish()
                         },
                         onDismiss = { finish() })
}
