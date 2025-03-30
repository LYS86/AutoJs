package com.tflite.yolo

import android.graphics.Bitmap
import android.graphics.RectF
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 图像预处理类，用于将输入图像转换为模型所需的格式
 * @property modelWidth 模型期望的输入宽度（默认640）
 * @property modelHeight 模型期望的输入高度（默认640）
 * @property normalize 是否对像素值进行归一化（默认true，归一化到0-1范围）
 */
class ImageProcessor(
    private val modelWidth: Int = 640,
    private val modelHeight: Int = 640,
    private val normalize: Boolean = true
) {
    private var scale = 1f
    private var padX = 0f
    private var padY = 0f
    private var origWidth = 0
    private var origHeight = 0

    private val srcMat = Mat()
    private val rgbMat = Mat()
    private val resizedMat = Mat()
    private val paddedMat = Mat(modelHeight, modelWidth, CvType.CV_8UC3)
    private val floatMat = Mat()

    companion object {
        init {
            if (!OpenCVLoader.initLocal()) {
                throw RuntimeException("OpenCV初始化失败")
            }
        }
    }

    /**
     * 预处理输入图像，转换为模型可接受的格式
     * @param bitmap 输入的Bitmap图像
     * @return 包含预处理后图像的ByteBuffer，可直接用于模型推理
     */
    fun process(bitmap: Bitmap): ByteBuffer {
        origWidth = bitmap.width
        origHeight = bitmap.height

        try {
            Utils.bitmapToMat(bitmap, srcMat)
            when (srcMat.channels()) {
                4 -> Imgproc.cvtColor(srcMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
                1 -> Imgproc.cvtColor(srcMat, rgbMat, Imgproc.COLOR_GRAY2RGB)
                else -> srcMat.copyTo(rgbMat)
            }

            calculateLetterboxParams(rgbMat)

            val scaledW = (rgbMat.cols() * scale).toInt()
            val scaledH = (rgbMat.rows() * scale).toInt()
            Imgproc.resize(
                rgbMat, resizedMat,
                Size(scaledW.toDouble(), scaledH.toDouble()),
                0.0, 0.0, Imgproc.INTER_LINEAR
            )

            paddedMat.setTo(Scalar(0.0, 0.0, 0.0))
            val roi = paddedMat.submat(
                padY.toInt(), padY.toInt() + scaledH,
                padX.toInt(), padX.toInt() + scaledW
            )
            resizedMat.copyTo(roi)
            roi.release()

            paddedMat.convertTo(floatMat, CvType.CV_32FC3, if (normalize) 1.0 / 255.0 else 1.0)

            return matToBuffer(floatMat)
        } finally {
            srcMat.release()
            rgbMat.release()
            resizedMat.release()
        }
    }

    /**
     * 将模型输出的归一化坐标(0-1范围)转换为原始图像坐标
     * @param normRect 归一化坐标矩形框
     * @return 对应原始图像中的矩形框坐标
     */
    fun normToOrig(normRect: RectF): RectF {
        val centerX = normRect.centerX() * modelWidth
        val centerY = normRect.centerY() * modelHeight
        return absToOrig(RectF(
            centerX - normRect.width() * modelWidth / 2,
            centerY - normRect.height() * modelHeight / 2,
            centerX + normRect.width() * modelWidth / 2,
            centerY + normRect.height() * modelHeight / 2
        ))
    }

    /**
     * 将坐标转换为原始图像坐标
     * @param absRect 模型输入空间的矩形框坐标
     * @return 对应原始图像中的矩形框坐标
     */
    fun absToOrig(absRect: RectF): RectF {
        val unpadX = (absRect.centerX() - padX) / scale
        val unpadY = (absRect.centerY() - padY) / scale
        val unpadW = absRect.width() / scale
        val unpadH = absRect.height() / scale

        return RectF(
            maxOf(0f, unpadX - unpadW / 2),
            maxOf(0f, unpadY - unpadH / 2),
            minOf(origWidth.toFloat(), unpadX + unpadW / 2),
            minOf(origHeight.toFloat(), unpadY + unpadH / 2)
        )
    }

    /**
     * 计算图像缩放比例和填充参数
     * @param mat 输入的OpenCV Mat对象
     */
    private fun calculateLetterboxParams(mat: Mat) {
        scale = minOf(
            modelWidth / mat.cols().toFloat(),
            modelHeight / mat.rows().toFloat()
        )
        val scaledW = (mat.cols() * scale).toInt()
        val scaledH = (mat.rows() * scale).toInt()
        padX = (modelWidth - scaledW) / 2f
        padY = (modelHeight - scaledH) / 2f
    }

    /**
     * 将OpenCV Mat转换为直接ByteBuffer
     * @param mat 输入的OpenCV Mat对象（必须是CV_32FC3类型）
     * @return 包含Mat数据的ByteBuffer
     * @throws IllegalArgumentException 如果输入Mat类型不是CV_32FC3
     */
    private fun matToBuffer(mat: Mat): ByteBuffer {
        require(mat.type() == CvType.CV_32FC3) { "输入必须是CV_32FC3类型" }

        val elementCount = mat.rows() * mat.cols() * 3
        val floatArray = FloatArray(elementCount).apply {
            mat.get(0, 0, this)
        }
        return ByteBuffer.allocateDirect(elementCount * 4)
            .order(ByteOrder.nativeOrder())
            .apply { asFloatBuffer().put(floatArray) }
    }

    /**
     * 释放所有OpenCV Mat资源
     */
    fun release() {
        srcMat.release()
        rgbMat.release()
        resizedMat.release()
        paddedMat.release()
        floatMat.release()
    }
}