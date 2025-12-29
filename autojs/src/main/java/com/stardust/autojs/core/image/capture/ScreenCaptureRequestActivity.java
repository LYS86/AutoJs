package com.stardust.autojs.core.image.capture;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.stardust.app.OnActivityResultDelegate;
import com.stardust.util.IntentExtras;

import timber.log.Timber;

/**
 * Created by Stardust on 2017/5/22.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScreenCaptureRequestActivity extends Activity {

    private OnActivityResultDelegate.Mediator mOnActivityResultDelegateMediator = new OnActivityResultDelegate.Mediator();
    private ScreenCaptureRequester mScreenCaptureRequester;
    private ScreenCaptureRequester.Callback mCallback;
    private int mResultCode = Activity.RESULT_CANCELED;
    private Intent mResultData = null;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (mCallback != null) {
                mCallback.onRequestResult(mResultCode, mResultData);
            }
            try {
                unbindService(this);
            } catch (Exception e) {
                Timber.e(e);
            }
            finish();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (mCallback != null) {
                mCallback.onRequestResult(Activity.RESULT_CANCELED, null);
            }
        }
    };

    public static void request(Context context, ScreenCaptureRequester.Callback callback) {
        Intent intent = new Intent(context, ScreenCaptureRequestActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentExtras.newExtras()
                .put("callback", callback)
                .putInIntent(intent);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentExtras extras = IntentExtras.fromIntentAndRelease(getIntent());
        if (extras == null) {
            finish();
            return;
        }
        mCallback = extras.get("callback");
        if (mCallback == null) {
            finish();
            return;
        }
        mScreenCaptureRequester = new ScreenCaptureRequester.ActivityScreenCaptureRequester(mOnActivityResultDelegateMediator, this);
        mScreenCaptureRequester.setOnActivityResultCallback(mCallback);
        mScreenCaptureRequester.request();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCallback = null;
        if (mScreenCaptureRequester == null)
            return;
        mScreenCaptureRequester.cancel();
        mScreenCaptureRequester = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mResultCode = resultCode;
        mResultData = data;

        if (resultCode == Activity.RESULT_OK) {
            Intent serviceIntent = new Intent(this, MediaProjectionService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
            bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            if (mCallback != null) {
                mCallback.onRequestResult(resultCode, data);
            }
            finish();
        }
    }

}