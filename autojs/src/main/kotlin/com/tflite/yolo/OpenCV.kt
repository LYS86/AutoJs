package com.tflite.yolo

import org.opencv.android.OpenCVLoader
import timber.log.Timber

object OpenCV {
    var isInit = false

    init {
        isInit = OpenCVLoader.initLocal().also {
            Timber.d("OpenCV init: $it")
            if (!it) {
                Timber.w("OpenCV init failed")
            }
        }
    }
}