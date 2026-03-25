package com.stardust.autojs.core.opencv

import org.opencv.android.OpenCVLoader

object OpenCVHelper {

    @JvmStatic
    val isLoaded: Boolean by lazy {
        OpenCVLoader.initLocal()
    }

    @JvmStatic
    fun ensureLoaded() {
        check(isLoaded) { "OpenCV init failed" }
    }

    @JvmStatic
    fun newMatOfPoint(mat: Mat): MatOfPoint = MatOfPoint(mat)

    @JvmStatic
    fun release(mat: MatOfPoint?) {
        mat?.release()
    }

    @JvmStatic
    fun release(mat: Mat?) {
        mat?.release()
    }
}
