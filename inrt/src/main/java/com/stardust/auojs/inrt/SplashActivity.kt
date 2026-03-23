package com.stardust.auojs.inrt

import android.Manifest
import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stardust.auojs.inrt.autojs.AutoJs
import com.stardust.auojs.inrt.launch.GlobalProjectLauncher
import com.stardust.autojs.permission.PermissionManager

/**
 * Created by Stardust on 2018/2/2.
 */

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val slug = findViewById<TextView>(R.id.slug)
        slug.typeface = Typeface.createFromAsset(assets, "roboto_medium.ttf")
        if (!Pref.isFirstUsing) {
            main()
        } else {
            Handler().postDelayed({ this@SplashActivity.main() }, INIT_TIMEOUT)
        }
    }

    private fun main() {
        requestPhonePermission()
        requestStoragePermission()
    }


    private fun runScript() {
        Thread {
            try {
                GlobalProjectLauncher.launch(this)
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SplashActivity, e.message, Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SplashActivity, LogActivity::class.java))
                    AutoJs.instance.globalConsole.printAllStackTrace(e)
                }
            }
        }.start()
    }

    private fun requestPhonePermission() {
        PermissionManager.requestRuntime(
            this, Manifest.permission.READ_PHONE_STATE
        ) {}
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            PermissionManager.requestRuntime(
                this, WRITE_EXTERNAL_STORAGE
            ) { isGranted ->
                if (isGranted) runScript()
            }
        } else {
            PermissionManager.requestSpecial(
                this, MANAGE_EXTERNAL_STORAGE
            ) { isGranted ->
                if (isGranted) runScript()
            }
        }
    }

    companion object {
        private const val INIT_TIMEOUT: Long = 2500
    }

}

