package org.autojs.autojs.external.tile

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import com.stardust.app.GlobalAppContext
import com.stardust.view.accessibility.AccessibilityService
import com.stardust.view.accessibility.LayoutInspector
import com.stardust.view.accessibility.NodeInfo
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.floating.FullScreenFloatyWindow
import timber.log.Timber

abstract class LayoutInspectTileService : TileService(), LayoutInspector.CaptureAvailableListener {

    private var isCapturing = false

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")
        AutoJs.getInstance().layoutInspector.addCaptureAvailableListener(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        Timber.d("onStartListening")
        setInactive()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        AutoJs.getInstance().layoutInspector.removeCaptureAvailableListener(this)
    }

    override fun onClick() {
        super.onClick()
        Timber.d("onClick")
        val service = AccessibilityService.instance ?: run {
            openAccessibilitySettingsAndCollapse()
            Toast.makeText(this, R.string.text_no_accessibility_permission_to_capture, Toast.LENGTH_SHORT).show()
            setInactive()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            service.performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            @Suppress("DEPRECATION")
            @SuppressLint("MissingPermission")
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }
        isCapturing = true
        GlobalAppContext.postDelayed({ AutoJs.getInstance().layoutInspector.captureCurrentWindow() }, 1000)
    }

    protected open fun setInactive() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    private fun openAccessibilitySettingsAndCollapse() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val wrapper = PendingIntentActivityWrapper(this, 0, intent, 0, false)
        TileServiceCompat.startActivityAndCollapse(this, wrapper)
    }

    override fun onCaptureAvailable(capture: NodeInfo?) {
        Timber.d("onCaptureAvailable: isCapturing=$isCapturing")
        if (!isCapturing) return
        isCapturing = false
        val nodeInfo = capture ?: return
        GlobalAppContext.post {
            val window = onCreateWindow(nodeInfo)
            if (!FloatyWindowManger.addWindow(this, window)) {
                setInactive()
            }
        }
    }

    protected abstract fun onCreateWindow(capture: NodeInfo): FullScreenFloatyWindow
}
