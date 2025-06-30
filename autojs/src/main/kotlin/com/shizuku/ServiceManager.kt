package com.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.BuildConfig
import com.stardust.autojs.runtime.api.AbstractShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
        ).daemon(false).processNameSuffix("adb_service").debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
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


    fun exit() {
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        iUserService = null
    }

    suspend fun waitForService() {
        if (!isBinding.get()) {
            bindService()
        }

        withTimeout(10_000) {
            while (!isBinding.get()) {
                delay(100)
            }
        }
    }

    suspend fun exec(command: String): AbstractShell.Result {
        return try {
            waitForService()
            val service = iUserService
            if (service == null) {
                return AbstractShell.Result().apply {
                    error = "Service not connected"
                }
            }
            withContext(Dispatchers.IO) {
                service.exec(command).let { json ->
                    AbstractShell.Result.ofJson(json)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Command execution failed")
            AbstractShell.Result().apply {
                error = e.message ?: "Unknown error"
            }
        }
    }
}