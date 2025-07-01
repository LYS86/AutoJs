package org.autojs.autojs.external.tile;

import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.stardust.app.GlobalAppContext;
import com.stardust.view.accessibility.AccessibilityService;
import com.stardust.view.accessibility.LayoutInspector;
import com.stardust.view.accessibility.NodeInfo;

import org.autojs.autojs.R;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.tool.AccessibilityServiceTool3;
import org.autojs.autojs.ui.floating.FloatyWindowManagerV2;
import org.autojs.autojs.ui.floating.FullScreenFloatyWindow;

import timber.log.Timber;

@RequiresApi(api = Build.VERSION_CODES.N)
public abstract class LayoutInspectTileService extends TileService implements LayoutInspector.CaptureAvailableListener {

    private boolean mCapturing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(getClass().getName(), "onCreate");
        AutoJs.getInstance().getLayoutInspector().addCaptureAvailableListener(this);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        Log.d(getClass().getName(), "onStartListening");
        inactive();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(getClass().getName(), "onDestroy");
        AutoJs.getInstance().getLayoutInspector().removeCaptureAvailableListener(this);
    }

    @Override
    public void onClick() {
        super.onClick();
        Timber.d("onClick");
//        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        boolean isRunning = AccessibilityService.Companion.isRunning();
        if (!isRunning) {
            Toast.makeText(this, R.string.text_no_accessibility_permission_to_capture, Toast.LENGTH_SHORT).show();
            AccessibilityServiceTool3.INSTANCE.toSetting();
            inactive();
            return;
        }

        AccessibilityService service = AccessibilityService.Companion.getInstance();
        if (service != null) {
            service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
        }
        mCapturing = true;
        GlobalAppContext.postDelayed(() ->
                        AutoJs.getInstance().getLayoutInspector().captureCurrentWindow()
                , 1000);
    }

    protected void inactive() {
        Tile qsTile = getQsTile();
        if (qsTile == null)
            return;
        qsTile.setState(Tile.STATE_INACTIVE);
        qsTile.updateTile();
    }

    @Override
    public void onCaptureAvailable(NodeInfo capture) {
        Log.d(getClass().getName(), "onCaptureAvailable: capturing = " + mCapturing);
        if (!mCapturing) {
            return;
        }
        mCapturing = false;
        GlobalAppContext.post(() -> {
            FullScreenFloatyWindow window = onCreateWindow(capture);
            if (!FloatyWindowManagerV2.INSTANCE.addWindow(getApplicationContext(), window)) {
                inactive();
            }
        });

    }

    protected abstract FullScreenFloatyWindow onCreateWindow(NodeInfo capture);
}
