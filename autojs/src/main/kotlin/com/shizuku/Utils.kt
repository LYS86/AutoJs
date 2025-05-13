package com.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.stardust.autojs.runtime.api.AbstractShell
import rikka.shizuku.Shizuku
import java.util.function.Consumer

class Utils(private val context: Context) {
    companion object {
        private const val TAG = "ShellUtils"
        private const val PERMISSION_CODE = 1234
        private const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
    }

    init {
        ServiceManager.initialize(context)
    }

    fun hasApp(): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
        true
    } catch (e: Exception) {
        false
    }

    fun launchApp(): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isReady(): Boolean {
        return Shizuku.pingBinder()
    }

    fun hasPermission(): Boolean {
        if (!isReady()) return false
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(callback: Consumer<Boolean>) {
        requestPermission { granted -> callback.accept(granted) }
    }

    fun requestPermission(): Boolean {
        val latch = java.util.concurrent.CountDownLatch(1)
        var granted = false

        requestPermission { result ->
            granted = result
            latch.countDown()
        }

        latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
        return granted
    }

    fun requestPermission(callback: (Boolean) -> Unit) {
        if (!isReady()) {
            callback(false)
            return
        }
        if (hasPermission()) {
            callback(true)
            return
        }
        if (Shizuku.isPreV11()) {
            callback(false)
            return
        }
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode == PERMISSION_CODE) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    callback(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(PERMISSION_CODE)
    }

    fun exec(command: String): AbstractShell.Result {
        return ServiceManager.exec(command)
    }

    fun exit() {
        ServiceManager.exit()
    }
}