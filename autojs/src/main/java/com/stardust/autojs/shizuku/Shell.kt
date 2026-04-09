package com.stardust.autojs.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.core.compat.getPackageInfoCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

object Shell {

    private const val PERMISSION_GRANTED = 0
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    private var mService: IShellService? = null
    private val binding = AtomicBoolean(false)

    private var mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            mService = IShellService.Stub.asInterface(binder)
            binding.set(false)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
        }
    }

    private val mArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(GlobalAppContext.get().packageName, ShellUserService::class.java.name)
        ).daemon(false).processNameSuffix("shizuku").debuggable(true).version(1)
    }

    fun isInstalled(): Boolean {
        return try {
            GlobalAppContext.get().packageManager.getPackageInfoCompat(SHIZUKU_PACKAGE)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e)
            false
        }
    }

    fun isRunning(): Boolean = Shizuku.pingBinder()

    fun requestPermission(requestCode: Int) {
        if (!isInstalled()) throw IllegalStateException("Shizuku app is not installed")
        if (!isRunning()) throw IllegalStateException("Shizuku is not running")
        if (checkPermission()) {
            bindUserService()
            return
        }
        Shizuku.requestPermission(requestCode)
    }

    fun checkPermission(): Boolean {
        return Shizuku.checkSelfPermission() == PERMISSION_GRANTED
    }

    fun unbindService() {
        mService = null
        binding.set(false)
        Shizuku.unbindUserService(mArgs, mConnection, true)
    }

    fun exec(command: String): Result = runBlocking { execSuspend(command) }

    suspend fun execSuspend(command: String): Result {
        return when {
            isInstalled().not() -> Result(-1, "Shizuku app is not installed", "")
            isRunning().not() -> Result(-1, "Shizuku is not running", "")
            checkPermission().not() -> Result(-1, "Shizuku permission denied", "")
            else -> bingAndWait().exec(command)
        }
    }

    suspend fun bingAndWait(timeout: Long = 10_000): IShellService {
        bindUserService()
        withTimeout(timeout) {
            while (mService == null) {
                delay(100)
            }
        }
        return mService ?: throw IllegalStateException("Shizuku service not bound")
    }

    fun bindUserService() {
        if (mService != null) return
        if (binding.compareAndSet(false, true)) {
            try {
                Shizuku.bindUserService(mArgs, mConnection)
            } catch (e: Exception) {
                binding.set(false)
                throw e
            }
        }
    }
}
