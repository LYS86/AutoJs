package com.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.stardust.autojs.runtime.api.AbstractShell
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 代码实现参考了以下博客内容，感谢作者的分享：
 * [Shizuku开发](https://blog.xxin.xyz/2024/04/28/Shizuku%E5%BC%80%E5%8F%91/)
 */
object ServiceManager {
    private const val TAG = "ServiceManager"
    private var iUserService: IUserService? = null
    private val isBinding = AtomicBoolean(false)

    private lateinit var userServiceArgs: UserServiceArgs

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

    fun initialize(context: Context) {
        userServiceArgs = UserServiceArgs(
            ComponentName(context.packageName, UserService::class.java.name)
        ).daemon(false).processNameSuffix("adb_service")
    }

    private fun bindService() {
        Shizuku.bindUserService(userServiceArgs, serviceConnection)
    }

    @Throws(RuntimeException::class)
    private fun waitForService() {
        if (!isBinding.get()) {
            bindService()
        }
        val startTime = System.currentTimeMillis()
        while (!isBinding.get()) {
            if (System.currentTimeMillis() - startTime > 10000) {
                throw RuntimeException("Service connection timeout")
            }
        }
    }

    fun exec(command: String): AbstractShell.Result {
        return try {
            waitForService()
            iUserService?.exec(command)?.let { json ->
                AbstractShell.Result.ofJson(json)
            } ?: throw RemoteException("Service not connected")
        } catch (e: Exception) {
            AbstractShell.Result().apply { error = e.message }
        }
    }

    fun exit() {
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        iUserService = null
    }
}