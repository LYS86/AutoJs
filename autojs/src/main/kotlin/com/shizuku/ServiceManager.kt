package com.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.runtime.api.AbstractShell
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

object ServiceManager {
    private var iUserService: IUserService? = null
    private val isBinding = AtomicBoolean(false)
    private var userServiceArgs: UserServiceArgs

    init {
        val context = GlobalAppContext.get()
        userServiceArgs = UserServiceArgs(
            ComponentName(context.packageName, UserService::class.java.name)
        ).daemon(false).processNameSuffix("adb_service")
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            if (binder.pingBinder()) {
                iUserService = IUserService.Stub.asInterface(binder)
                isBinding.set(true)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBinding.set(false)
            iUserService = null
        }
    }

    private fun bindService() {
        Shizuku.bindUserService(userServiceArgs, serviceConnection)
    }

    private fun waitForService() {
        if (!isBinding.get()) {
            bindService()
        }
        val startTime = System.currentTimeMillis()
        while (!isBinding.get()) {
            if (System.currentTimeMillis() - startTime > 10000) {
                throw RuntimeException("Service connection timeout")
            }
            Thread.sleep(100)
        }
    }

    fun exec(command: String): AbstractShell.Result {
        return try {
            waitForService()
            iUserService?.exec(command)?.let { json ->
                AbstractShell.Result.ofJson(json).also {
                    Timber.d("result: $it")
                }
            } ?: run {
                Timber.e("Service not connected")
                AbstractShell.Result().apply {
                    error = "Service not connected"
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Command execution failed")
            AbstractShell.Result().apply {
                error = e.message
            }
        }
    }

    fun exit() {
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        iUserService = null
    }
}