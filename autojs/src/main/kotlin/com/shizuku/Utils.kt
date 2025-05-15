package com.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.runtime.api.AbstractShell
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object Utils {
    private const val PERMISSION_CODE = 1234
    private const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"

    private val context: Context
        get() = GlobalAppContext.get()

    fun hasApp(): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
        true
    } catch (e: Exception) {
        Timber.w(e, "Check Shizuku app failed")
        false
    }

    fun launchApp(): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Timber.d("Launch Shizuku app")
                true
            } else {
                Timber.w("No launch intent for Shizuku")
                false
            }
        } catch (e: Exception) {
            Timber.w(e, "Launch Shizuku app failed")
            false
        }
    }

    fun isReady(): Boolean = Shizuku.pingBinder()

    fun hasPermission(): Boolean =
        isReady() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun requestPermission(callback: Consumer<Boolean>) {
        requestPermission { granted -> callback.accept(granted) }
    }

    fun requestPermission(): Boolean {
        val latch = CountDownLatch(1)
        var granted = false

        requestPermission { result ->
            granted = result
            latch.countDown()
        }

        try {
            latch.await(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Timber.e(e, "Permission request interrupted")
        }
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
        if (!hasPermission()) return AbstractShell.Result().apply {
            error = "No permission or Shizuku not running"
        }.also { Timber.d("result: $it") }
        return ServiceManager.exec(command)
    }

    /**
     * 协程版本的命令执行
     */
    suspend fun exec2(command: String): AbstractShell.Result {
        if (!hasPermission()) return AbstractShell.Result().apply {
            error = "No permission or Shizuku not running"
        }.also { Timber.d("result: $it") }
        return ServiceManager.exec2(command)
    }

    fun exit() {
        ServiceManager.exit()
    }
}