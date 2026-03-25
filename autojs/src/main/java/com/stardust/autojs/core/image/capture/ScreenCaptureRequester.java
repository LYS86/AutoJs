package com.stardust.autojs.core.image.capture;

import android.app.Activity;
import android.content.Intent;

/**
 * Created by Stardust on 2017/5/17.
 */

public interface ScreenCaptureRequester {

    void cancel();

    interface Callback {

        void onRequestResult(int result, Intent data);

    }

    void request();

    void setOnActivityResultCallback(Callback callback);

    abstract class AbstractScreenCaptureRequester implements ScreenCaptureRequester {

        protected Callback mCallback;
        protected Intent mResult;

        @Override
        public void setOnActivityResultCallback(Callback callback) {
            mCallback = callback;
        }

        public void onResult(int resultCode, Intent data) {
            mResult = data;
            if (mCallback != null)
                mCallback.onRequestResult(resultCode, data);
        }

        @Override
        public void cancel() {
            if (mResult != null)
                return;
            if (mCallback != null)
                mCallback.onRequestResult(Activity.RESULT_CANCELED, null);
        }
    }

}
