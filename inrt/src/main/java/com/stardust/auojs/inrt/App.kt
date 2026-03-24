package com.stardust.auojs.inrt

import android.app.Application
import com.stardust.app.GlobalAppContext
import com.stardust.auojs.inrt.autojs.AutoJs
import com.stardust.auojs.inrt.autojs.GlobalKeyObserver
import timber.log.Timber

/**
 * Created by Stardust on 2017/7/1.
 */

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        GlobalAppContext.set(this)
        AutoJs.initInstance(this)
        GlobalKeyObserver.init()
        Timber.plant(Timber.DebugTree())
    }

}
