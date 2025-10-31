package com.stardust.app

import android.app.Application
import android.content.Context
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Stardust on 2018/3/22.
 */
object GlobalAppContext {

    private var applicationContext: Context? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    @JvmStatic
    fun set(application: Application) {
        applicationContext = application.applicationContext
    }

    @JvmStatic
    fun get(): Context {
        return applicationContext ?: throw IllegalStateException("Call GlobalAppContext.set() to set a application context")
    }

    @JvmStatic
    fun getString(resId: Int): String = get().getString(resId)

    @JvmStatic
    fun getString(resId: Int, vararg formatArgs: Any): String = get().getString(resId, *formatArgs)

    @JvmStatic
    fun getColor(id: Int): Int =  ContextCompat.getColor(get(), id)

    @JvmStatic
    fun toast(message: String) = showToast { Toast.makeText(get(), message, Toast.LENGTH_SHORT).show() }

    @JvmStatic
    fun toast(resId: Int) = showToast { Toast.makeText(get(), resId, Toast.LENGTH_SHORT).show() }

    @JvmStatic
    fun toast(resId: Int, vararg args: Any) {
        val message = getString(resId, *args)
        showToast { Toast.makeText(get(), message, Toast.LENGTH_SHORT).show() }
    }

    @JvmStatic
    fun post(r: Runnable) = mainScope.launch { r.run() }

    @JvmStatic
    fun postDelayed(r: Runnable, delayMillis: Long) = mainScope.launch {
        kotlinx.coroutines.delay(delayMillis)
        r.run()
    }

    private fun showToast(showAction: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showAction()
        } else {
            mainScope.launch { showAction() }
        }
    }
}