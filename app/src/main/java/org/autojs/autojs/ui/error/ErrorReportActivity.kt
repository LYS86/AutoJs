package org.autojs.autojs.ui.error

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stardust.autojs.compose.layout.TopAppBarScaffold
import com.stardust.autojs.compose.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.autojs.autojs.BuildConfig
import org.autojs.autojs.R

class ErrorReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })
        val message = intent.getStringExtra("message") ?: ""
        val error = intent.getStringExtra("error") ?: ""
        setContent {
            AppTheme {
                ErrorReportScreen(
                    message = message,
                    error = error,
                    onCopyDebugInfo = { debugText ->
                        val clipboard = getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("Debug", debugText))
                    },
                    onExit = { finishAffinity() }
                )
            }
        }
    }
}

@Composable
fun ErrorReportScreen(
    message: String,
    error: String,
    onCopyDebugInfo: (String) -> Unit,
    onExit: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    TopAppBarScaffold(
        title = stringResource(R.string.text_crash),
        onBack = onExit
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.crash_feedback),
                style = MaterialTheme.typography.bodyMedium
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val deviceInfo = "Version: ${BuildConfig.VERSION_CODE}\nAndroid: ${Build.VERSION.SDK_INT}\n"
                        onCopyDebugInfo(deviceInfo + error)
                        copied = true
                        coroutineScope.launch {
                            delay(1000)
                            onExit()
                        }
                    },
                    enabled = !copied,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (copied) stringResource(R.string.text_already_copy_to_clip) else stringResource(R.string.text_copy_debug_info)
                    )
                }

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.text_exit))
                }
            }
        }
    }
}
