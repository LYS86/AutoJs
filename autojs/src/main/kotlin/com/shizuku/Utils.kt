package com.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.runtime.api.AbstractShell
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds

object Utils {
    private const val PERMISSION_CODE = 1234
    private const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
    private const val DEFAULT_TIMEOUT_SECONDS = 10L

    private val context: Context
        get() = GlobalAppContext.get()

    private val permissionListeners = ConcurrentHashMap<Int, Continuation<Boolean>>()

    init {
        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == PERMISSION_CODE) {
                permissionListeners.remove(requestCode)
                    ?.resume(grantResult == PackageManager.PERMISSION_GRANTED)
            }
        }
    }

    /**
     * 请求shizuku权限
     * @param timeoutSeconds 超时时间，单位秒
     * @return 是否成功获取权限
     */
    suspend fun requestPermissionSuspend(timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS): Boolean {
        if (!isReady()) return false
        if (hasPermission()) return true
        if (Shizuku.isPreV11()) return false

        return try {
            withTimeout(timeoutSeconds.seconds) {
                suspendCoroutine { continuation ->
                    permissionListeners[PERMISSION_CODE] = continuation
                    Shizuku.requestPermission(PERMISSION_CODE)
                }
            }
        } catch (e: TimeoutCancellationException) {
            permissionListeners.remove(PERMISSION_CODE)
            Timber.w("Permission request timed out after ${timeoutSeconds}s")
            false
        } catch (e: Exception) {
            permissionListeners.remove(PERMISSION_CODE)
            Timber.e(e, "Permission request failed")
            false
        }
    }

    fun requestPermission(timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS): Boolean {
        return runBlocking {
            try {
                requestPermissionSuspend(timeoutSeconds)
            } catch (e: Exception) {
                Timber.e(e, "Request permission sync failed")
                false
            }
        }
    }

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

    fun exec(command: String) = runBlocking {
        execSuspend(command)
    }

    suspend fun execSuspend(command: String): AbstractShell.Result {
        if (!hasPermission()) return AbstractShell.Result().apply {
            error = "No permission or Shizuku not running"
        }
        return ServiceManager.exec(command)
    }

    fun exit() {
        ServiceManager.exit()
    }
}