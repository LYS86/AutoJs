package com.stardust.auojs.inrt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.stardust.auojs.inrt.autojs.AutoJs
import com.stardust.auojs.inrt.launch.GlobalProjectLauncher
import com.stardust.autojs.R.color
import com.stardust.autojs.compose.theme.AppTheme
import com.stardust.autojs.core.console.ConsoleImpl
import com.stardust.autojs.core.console.ConsoleView
import com.stardust.autojs.engine.ScriptEngine
import com.stardust.autojs.engine.ScriptEngineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class LogActivity : ComponentActivity() {

    private var isScriptRunning by mutableStateOf(false)
    private var snackbarMessage by mutableStateOf<String?>(null)

    private val engineLifecycleCallback = object : ScriptEngineManager.EngineLifecycleCallback {
        override fun onEngineCreate(engine: ScriptEngine<*>) {
            isScriptRunning = true
        }

        override fun onEngineRemove(engine: ScriptEngine<*>) {
            isScriptRunning = AutoJs.instance.scriptEngineManager.engines.isNotEmpty()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                LaunchedEffect(snackbarMessage) {
                    snackbarMessage?.let { message ->
                        snackbarHostState.showSnackbar(message)
                        snackbarMessage = null
                    }
                }

                LogScreen(
                    isScriptRunning = isScriptRunning,
                    onRunClick = ::launchScript,
                    onStopClick = ::stopScript,
                    onClearClick = ::clearLog,
                    onSettingsClick = {
                        startActivity(Intent(this@LogActivity, SettingsActivity::class.java))
                    },
                    console = AutoJs.instance.globalConsole,
                    snackbarHostState = snackbarHostState
                )
            }
        }
        init(savedInstanceState == null)
    }

    override fun onDestroy() {
        AutoJs.instance.scriptEngineService.unregisterEngineLifecycleCallback(engineLifecycleCallback)
        super.onDestroy()
    }

    private fun init(isFirstCreate: Boolean) {
        if (isFirstCreate.not()) return
        AutoJs.instance.scriptEngineService.registerEngineLifecycleCallback(engineLifecycleCallback)
        isScriptRunning = AutoJs.instance.scriptEngineManager.engines.isNotEmpty()
        if (BuildConfig.DEBUG) {
            launchScript()
        }
    }

    private fun launchScript() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    GlobalProjectLauncher.launch(this@LogActivity)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to launch script")
                showSnackbar(e.message ?: "Unknown error")
                AutoJs.instance.globalConsole.printAllStackTrace(e)
            }
        }
    }

    private fun stopScript() {
        AutoJs.instance.scriptEngineService.stopAll()
        showSnackbar(getString(R.string.text_script_stopped))
    }

    private fun clearLog() {
        (AutoJs.instance.globalConsole as? ConsoleImpl)?.clear()
        showSnackbar(getString(R.string.text_log_cleared))
    }

    private fun showSnackbar(message: String) {
        snackbarMessage = message
    }

    companion object {
        const val EXTRA_LAUNCH_SCRIPT = "launch_script"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogScreen(
    isScriptRunning: Boolean,
    onRunClick: () -> Unit,
    onStopClick: () -> Unit,
    onClearClick: () -> Unit,
    onSettingsClick: () -> Unit,
    console: ConsoleImpl,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                scrolledContainerColor = MaterialTheme.colorScheme.primary
            ),
            actions = {
            IconButton(onClick = {
                if (isScriptRunning) {
                    onStopClick()
                } else {
                    onRunClick()
                }
            }) {
                Icon(
                    imageVector = if (isScriptRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(if (isScriptRunning) R.string.text_stop else R.string.text_run)
                )
            }
            IconButton(onClick = onClearClick) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.text_clear_log))
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.text_settings))
            }
        })
    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        ConsoleViewWrapper(
            console = console, modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun ConsoleViewWrapper(console: ConsoleImpl, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            ConsoleView(ctx).apply {
                val colors = SparseArray<Int>().apply {
                    put(Log.VERBOSE, ctx.getColor(color.md_theme_outlineVariant))
                    put(Log.DEBUG, ctx.getColor(color.md_theme_onSurface_highContrast))
                    put(Log.INFO, 0xff64dd17.toInt())
                    put(Log.WARN, 0xff2962ff.toInt())
                    put(Log.ERROR, 0xffd50000.toInt())
                    put(Log.ASSERT, 0xffff534e.toInt())
                }
                setColors(colors)
                setConsole(console)
                findViewById<View>(R.id.input_container).visibility = View.GONE
            }
        }, modifier = modifier
    )
}