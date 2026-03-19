package org.autojs.autojs

import android.annotation.SuppressLint
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.multidex.MultiDexApplication
import com.stardust.app.GlobalAppContext
import com.stardust.theme.ThemeColor
import com.tencent.bugly.Bugly
import com.tencent.bugly.crashreport.CrashReport
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.autojs.key.GlobalKeyObserver
import org.autojs.autojs.external.receiver.DynamicBroadcastReceivers
import org.autojs.autojs.theme.ThemeColorManagerCompat
import org.autojs.autojs.timing.TimedTaskManager
import org.autojs.autojs.timing.TimedTaskScheduler
import org.autojs.autojs.tool.CrashHandler
import org.autojs.autojs.ui.error.ErrorReportActivity
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by Stardust on 2017/1/27.
 */

class App : MultiDexApplication() {
    lateinit var dynamicBroadcastReceivers: DynamicBroadcastReceivers
        private set

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        GlobalAppContext.set(this)
        instance = WeakReference(this)
        setUpDebugEnvironment()
        init()
    }

    private fun setUpDebugEnvironment() {
        Bugly.isDev = false
        val crashHandler = CrashHandler(ErrorReportActivity::class.java)

        val strategy = CrashReport.UserStrategy(applicationContext)
        strategy.setCrashHandleCallback(crashHandler)

        CrashReport.initCrashReport(applicationContext, BUGLY_APP_ID, false, strategy)

        crashHandler.setBuglyHandler(Thread.getDefaultUncaughtExceptionHandler())
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)

    }

    private fun init() {
        ThemeColorManagerCompat.init(ThemeColor(
            ContextCompat.getColor(this, R.color.colorPrimary),
            ContextCompat.getColor(this, R.color.colorPrimaryDark),
            ContextCompat.getColor(this, R.color.colorAccent)
        ))
        AutoJs.initInstance(this)
        if (Pref.isRunningVolumeControlEnabled()) {
            GlobalKeyObserver.init()
        }
        TimedTaskScheduler.init(this)
        initDynamicBroadcastReceivers()
    }

    @SuppressLint("CheckResult")
    private fun initDynamicBroadcastReceivers() {
        dynamicBroadcastReceivers = DynamicBroadcastReceivers(this)
        val localActions = ArrayList<String>()
        val actions = ArrayList<String>()
        TimedTaskManager.getInstance().allIntentTasks
                .filter { task -> task.action != null }
                .doOnComplete {
                    if (localActions.isNotEmpty()) {
                        dynamicBroadcastReceivers.register(localActions, true)
                    }
                    if (actions.isNotEmpty()) {
                        dynamicBroadcastReceivers.register(actions, false)
                    }
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(
                            DynamicBroadcastReceivers.ACTION_STARTUP
                    ))
                }
                .subscribe({
                    if (it.isLocal) {
                        localActions.add(it.action)
                    } else {
                        actions.add(it.action)
                    }
                }, {
                    Timber.e(it)
                })


    }

    companion object {

        private val TAG = "App"
        private val BUGLY_APP_ID = "19b3607b53"

        private lateinit var instance: WeakReference<App>

        val app: App
            get() = instance.get()!!
    }


}
