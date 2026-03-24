package com.stardust.auojs.inrt

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.stardust.auojs.inrt.autojs.AutoJs
import com.stardust.auojs.inrt.launch.GlobalProjectLauncher
import com.stardust.autojs.core.console.ConsoleImpl
import com.stardust.autojs.core.console.ConsoleView
import com.stardust.autojs.permission.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class LogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setupView()
        requestPermissionsAndLaunch()
    }

    private fun setupView() {
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val consoleView = findViewById<ConsoleView>(R.id.console)
        consoleView.setConsole(AutoJs.instance.globalConsole as ConsoleImpl)
        consoleView.findViewById<View>(R.id.input_container).visibility = View.GONE
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
            PermissionManager.requestSpecial(
                this, Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ) { _ ->
                onComplete()
            }
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
                Toast.makeText(this@LogActivity, e.message, Toast.LENGTH_LONG).show()
                AutoJs.instance.globalConsole.printAllStackTrace(e)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        startActivity(Intent(this, SettingsActivity::class.java))
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    companion object {
        const val EXTRA_LAUNCH_SCRIPT = "launch_script"
    }
}
