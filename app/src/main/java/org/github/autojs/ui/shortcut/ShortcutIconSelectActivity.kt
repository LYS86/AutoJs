package org.github.autojs.ui.shortcut

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.stardust.autojs.compose.theme.AppTheme
import com.stardust.autojs.core.compat.getApplicationInfoCompat
import com.stardust.autojs.core.compat.queryIntentActivitiesCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.github.autojs.shortcut.EXTRA_PACKAGE_NAME

class ShortcutIconSelectActivity : ComponentActivity() {

    private val appList = mutableStateListOf<AppItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadApps()
        setContent {
            AppTheme {
                ShortcutIconSelectContent(appList = appList, onAppSelected = { packageName ->
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_PACKAGE_NAME, packageName))
                    finish()
                }, onImagePicked = { uri ->
                    setResult(RESULT_OK, Intent().setData(uri))
                    finish()
                }, onBack = { finish() })
            }
        }
    }

    private fun loadApps() {
        lifecycleScope.launch(Dispatchers.Default) {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            val launchablePackages = packageManager
                .queryIntentActivitiesCompat(launcherIntent, 0)
                .map { it.activityInfo.packageName }
                .toSet()
            appList.clear()
            launchablePackages.forEach { pkg ->
                val info = packageManager.getApplicationInfoCompat(pkg)
                val drawable = info.loadIcon(packageManager)
                val bitmap = drawable.toBitmap()
                val appItem = AppItem(
                    packageName = info.packageName,
                    label = info.loadLabel(packageManager).toString(),
                    iconBitmap = bitmap
                )
                withContext(Dispatchers.Main) {
                    appList.add(appItem)
                }
            }
        }
    }
}

@Composable
private fun ShortcutIconSelectContent(
    appList: List<AppItem>, onAppSelected: (String) -> Unit, onImagePicked: (Uri) -> Unit, onBack: () -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onImagePicked(uri)
    }
    ShortcutIconSelectScreen(appList = appList,
                             onAppSelected = onAppSelected,
                             onPickImage = { imagePickerLauncher.launch("image/*") },
                             onBack = onBack)
}
