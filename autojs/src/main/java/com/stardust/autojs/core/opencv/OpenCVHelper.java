package com.stardust.autojs.core.opencv;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.Nullable;

import org.opencv.android.OpenCVLoader;


/**
 * Created by Stardust on 2018/4/2.
 */

public class OpenCVHelper {

    private static final String LOG_TAG = "OpenCVHelper";
    private static boolean sInitialized = false;

    public static MatOfPoint newMatOfPoint(Mat mat) {
        return new MatOfPoint(mat);
    }

    public static void release(@Nullable MatOfPoint mat) {
        if (mat == null)
            return;
        mat.release();
    }

    public static void release(@Nullable Mat mat) {
        if (mat == null)
            return;
        mat.release();
    }

    public synchronized static boolean isInitialized() {
        return sInitialized;
    }

    public synchronized static void initIfNeeded(Context context, InitializeCallback callback) {
        if (sInitialized) {
            callback.onInitFinish();
            return;
        }
        sInitialized = true;
        if (Looper.getMainLooper() == Looper.myLooper()) {
            new Thread(() -> {
                OpenCVLoader.initDebug();
                callback.onInitFinish();
            }).start();
        } else {
            OpenCVLoader.initDebug();
            callback.onInitFinish();
        }
    }

    public interface InitializeCallback {
        void onInitFinish();
    }
}
