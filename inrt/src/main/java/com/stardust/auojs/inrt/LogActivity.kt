package com.stardust.auojs.inrt

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.stardust.auojs.inrt.autojs.AutoJs
import com.stardust.auojs.inrt.databinding.ActivityMainBinding
import com.stardust.auojs.inrt.launch.GlobalProjectLauncher
import com.stardust.autojs.core.console.ConsoleImpl
import com.stardust.autojs.permission.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class LogActivity : androidx.appcompat.app.AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isScriptRunning = false
    private var runMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setupView()
        requestPermissionsAndLaunch()
    }

    private fun setupView() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            onMenuItemClick(menuItem)
            true
        }
        runMenuItem = binding.toolbar.menu.findItem(R.id.action_run)
        
        binding.console.setConsole(AutoJs.instance.globalConsole)
        binding.console.findViewById<View>(R.id.input_container).visibility = View.GONE
    }

    private fun onMenuItemClick(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_run -> {
                if (isScriptRunning) {
                    stopScript()
                } else {
                    launchScript()
                }
            }
            R.id.action_clear -> {
                clearLog()
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return false
    }

    private fun updateRunButton() {
        runMenuItem?.let { item ->
            if (isScriptRunning) {
                item.setIcon(R.drawable.ic_stop_24dp)
                item.setTitle(R.string.text_stop)
            } else {
                item.setIcon(R.drawable.ic_play_arrow_24dp)
                item.setTitle(R.string.text_run)
            }
        }
    }

    private fun requestPermissionsAndLaunch() {
        requestStoragePermission {
            launchScript()
        }
    }

    private fun requestStoragePermission(onComplete: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            PermissionManager.requestRuntime(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) { _ ->
                onComplete()
            }
        } else {
            PermissionManager.openSettings(
                this, Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ) { _ ->
                onComplete()
            }
        }
    }

    private fun launchScript() {
        isScriptRunning = true
        updateRunButton()
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    GlobalProjectLauncher.launch(this@LogActivity)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to launch script")
                showSnackbar(e.message ?: "Unknown error")
                AutoJs.instance.globalConsole.printAllStackTrace(e)
            } finally {
                isScriptRunning = false
                updateRunButton()
            }
        }
    }

    private fun stopScript() {
        AutoJs.instance.scriptEngineService.stopAll()
        isScriptRunning = false
        updateRunButton()
        showSnackbar(getString(R.string.text_script_stopped))
    }

    private fun clearLog() {
        (AutoJs.instance.globalConsole as? ConsoleImpl)?.clear()
        showSnackbar(getString(R.string.text_log_cleared))
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_LAUNCH_SCRIPT = "launch_script"
    }
}
