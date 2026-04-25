package org.autojs.autojs.autojs

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.runtime.ScriptRuntime
import com.stardust.autojs.runtime.api.AppUtils
import com.stardust.autojs.runtime.exception.ScriptException
import com.stardust.autojs.runtime.exception.ScriptInterruptedException
import com.stardust.autojs.util.AccessibilityServiceUtils
import com.stardust.autojs.util.FileProviderUtils
import com.stardust.view.accessibility.AccessibilityService
import com.stardust.view.accessibility.LayoutInspector
import com.stardust.view.accessibility.NodeInfo
import org.autojs.autojs.R
import org.autojs.autojs.ui.compose.settings.SettingsActivity
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.floating.FullScreenFloatyWindow
import org.autojs.autojs.ui.floating.layoutinspector.LayoutBoundsFloatyWindow
import org.autojs.autojs.ui.floating.layoutinspector.LayoutHierarchyFloatyWindow
import org.autojs.autojs.ui.log.LogActivity
import timber.log.Timber
import com.stardust.autojs.AutoJs as BaseAutoJs

class AutoJs private constructor(application: Application) : BaseAutoJs(application) {

    private val layoutInspectBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                ensureAccessibilityServiceEnabled()
                when (intent.action) {
                    LayoutBoundsFloatyWindow::class.java.name -> capture(::LayoutBoundsFloatyWindow)
                    LayoutHierarchyFloatyWindow::class.java.name -> capture(::LayoutHierarchyFloatyWindow)
                }
            } catch (e: Exception) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    throw e
                }
            }
        }
    }

    init {
        scriptEngineService.registerGlobalScriptExecutionListener(ScriptExecutionGlobalListener())
        val intentFilter = IntentFilter().apply {
            addAction(LayoutBoundsFloatyWindow::class.java.name)
            addAction(LayoutHierarchyFloatyWindow::class.java.name)
        }
        LocalBroadcastManager.getInstance(application).registerReceiver(layoutInspectBroadcastReceiver, intentFilter)
    }

    override fun createAppUtils(context: Context): AppUtils {
        return AppUtils(context, FileProviderUtils.getAuthority(context))
    }

    override fun ensureAccessibilityServiceEnabled() {
        if (AccessibilityService.instance != null) return
        val errorMessage = resolveAccessibilityErrorMessage()
        if (errorMessage.isEmpty()) return
        AccessibilityServiceUtils.openSetting()
        throw ScriptException(errorMessage)
    }

    override fun waitForAccessibilityServiceEnabled() {
        if (AccessibilityService.instance != null) return
        val errorMessage = resolveAccessibilityErrorMessage()
        if (errorMessage.isEmpty()) return
        AccessibilityServiceUtils.openSetting()
        if (!AccessibilityService.waitForEnabled(-1)) {
            throw ScriptInterruptedException()
        }
    }

    override fun createRuntime(): ScriptRuntime {
        val runtime = super.createRuntime()
        runtime.putProperty("class.settings", SettingsActivity::class.java)
        runtime.putProperty("class.console", LogActivity::class.java)
        runtime.putProperty("broadcast.inspect_layout_bounds", LayoutBoundsFloatyWindow::class.java.name)
        runtime.putProperty("broadcast.inspect_layout_hierarchy", LayoutHierarchyFloatyWindow::class.java.name)
        return runtime
    }

    private fun resolveAccessibilityErrorMessage(): String = when {
        AccessibilityServiceUtils.isEnabled() ->
            GlobalAppContext.getString(R.string.text_auto_operate_service_enabled_but_not_running).also {
                Timber.w(AccessibilityServiceUtils.readServices())
            }

        AccessibilityServiceUtils.isAutoEnable ->
            when {
                !AccessibilityServiceUtils.enableServiceAndWaitBlocking(2000) ->
                    GlobalAppContext.getString(R.string.text_auto_enable_service_timeout)

                else -> ""
            }

        else -> GlobalAppContext.getString(R.string.text_no_accessibility_permission)
    }

    private fun capture(windowFactory: (NodeInfo) -> FullScreenFloatyWindow) {
        val inspector = layoutInspector
        val listener = object : LayoutInspector.CaptureAvailableListener {
            override fun onCaptureAvailable(capture: NodeInfo?) {
                inspector.removeCaptureAvailableListener(this)
                if (capture != null) {
                    uiHandler.post {
                        FloatyWindowManger.addWindow(application.applicationContext, windowFactory(capture))
                    }
                }
            }
        }
        inspector.addCaptureAvailableListener(listener)
        if (!inspector.captureCurrentWindow()) {
            inspector.removeCaptureAvailableListener(listener)
        }
    }

    companion object {
        @Volatile
        private var _instance: AutoJs? = null

        @JvmStatic
        fun getInstance(): AutoJs = _instance!!

        @JvmStatic
        @Synchronized
        fun initInstance(application: Application) {
            if (_instance != null) return
            _instance = AutoJs(application)
        }
    }
}
