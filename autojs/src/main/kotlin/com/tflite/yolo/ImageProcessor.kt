package com.tflite.yolo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.stardust.autojs.core.image.ImageWrapper
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.TensorImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import org.tensorflow.lite.support.image.ImageProcessor as TfImageProcessor

class ImageProcessor private constructor() {

    enum class Mode {
        OPENCV,
        TENSORFLOW
    }

    private var modelWidth = 640
    private var modelHeight = 640
    private var normalizeMean = 0f
    private var normalizeStd = 255f
    private var mode = Mode.OPENCV
    private var stride = 32

    // 对象池
    private val matPool = MatObjectPool()
    private val bufferPool = BufferObjectPool()

    private var _opencvBuffer: ByteBuffer = ByteBuffer.allocateDirect(0)
    private var _opencvBitmap: Bitmap = createBitmap(1, 1)
    private val _tensorImage = TensorImage(DataType.FLOAT32)
    private val _tfImageProcessor = TfImageProcessor.Builder()
        .add(NormalizeOp(normalizeMean, normalizeStd))
        .build()
    private var _tfImage: TensorImage = TensorImage(DataType.FLOAT32)

    // 变换参数
    private var scale = 1f
    private var padX = 0f
    private var padY = 0f

    private val opencvAvailable = run {
        try {
            OpenCVLoader.initLocal()
            true
        } catch (e: Throwable) {
            false
        }
    }

    val buffer: ByteBuffer
        get() = when (mode) {
            Mode.TENSORFLOW -> _tfImage.buffer
            Mode.OPENCV -> _opencvBuffer
        }

    val bitmap: Bitmap
        get() = when (mode) {
            Mode.TENSORFLOW -> _tensorImage.bitmap
            Mode.OPENCV -> _opencvBitmap
        }

    companion object {
        private const val TAG = "ImageProcessor"
        private const val GRAY_FILL = 114

        fun create(): ImageProcessor {
            return ImageProcessor().apply {
                if (!opencvAvailable) {
                    mode = Mode.TENSORFLOW
                }
            }
        }
    }

    fun size(width: Int, height: Int): ImageProcessor {
        modelWidth = width
        modelHeight = height
        return this
    }

    fun normalize(mean: Float = 0f, std: Float = 255f): ImageProcessor {
        normalizeMean = mean
        normalizeStd = std
        return this
    }

    fun mode(mode: Mode): ImageProcessor {
        this.mode = if (mode == Mode.OPENCV && !opencvAvailable) Mode.TENSORFLOW else mode
        return this
    }

    fun ofBitmap(bitmap: Bitmap): ImageProcessor {
        when (mode) {
            Mode.TENSORFLOW -> processTensorFlow(bitmap)
            Mode.OPENCV -> processOpenCV(bitmap)
        }
        return this
    }

    fun save(path: String) {
        ImageWrapper.ofBitmap(bitmap).saveTo(path)
    }

    fun normToOrig(normRect: RectF): RectF {
        val absRect = RectF(
            normRect.left * modelWidth,
            normRect.top * modelHeight,
            normRect.right * modelWidth,
            normRect.bottom * modelHeight
        )
        return absToOrig(absRect)
    }

    fun absToOrig(absRect: RectF): RectF {
        return RectF(
            (absRect.left - padX).coerceAtLeast(0f) / scale,
            (absRect.top - padY).coerceAtLeast(0f) / scale,
            (absRect.right - padX).coerceAtMost(modelWidth.toFloat()) / scale,
            (absRect.bottom - padY).coerceAtMost(modelHeight.toFloat()) / scale
        )
    }

    fun release() {
        matPool.clear()
        bufferPool.clear()
    }

    private fun processOpenCV(bitmap: Bitmap) {
        val srcMat = matPool.acquire()
        val paddedMat = matPool.acquire()
        val normalizedMat = matPool.acquire()
        try {
            Utils.bitmapToMat(bitmap, srcMat)
            Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB)
            val (r, newUnpaid) = calculateScaleRatio(srcMat)
            scale = r.toFloat()
            val (usedPaddedMat, padding) = padImage(srcMat, newUnpaid, paddedMat)
            padX = padding.first.toFloat()
            padY = padding.second.toFloat()
            usedPaddedMat.convertTo(normalizedMat, CvType.CV_32FC3, 1.0 / 255.0)
            _opencvBuffer = convertMatToModelInput(normalizedMat)
            _opencvBitmap = createBitmap(modelWidth, modelHeight).apply {
                normalizedMat.convertTo(usedPaddedMat, CvType.CV_8UC3, 255.0)
                Utils.matToBitmap(usedPaddedMat, this)
            }
        } finally {
            matPool.release(srcMat)
            matPool.release(paddedMat)
            matPool.release(normalizedMat)
        }
    }

    private fun padImage(srcMat: Mat, newUnpaid: Size, targetMat: Mat): Pair<Mat, Pair<Int, Int>> {
        val resizedMat = matPool.acquire()
        try {
            Imgproc.resize(srcMat, resizedMat, newUnpaid)
            val dw = (modelWidth - newUnpaid.width).toInt()
            val dh = (modelHeight - newUnpaid.height).toInt()
            val dwAligned = (dw / stride) * stride
            val dhAligned = (dh / stride) * stride
            Core.copyMakeBorder(
                resizedMat, targetMat,
                dhAligned / 2, dhAligned - dhAligned / 2,
                dwAligned / 2, dwAligned - dwAligned / 2,
                Core.BORDER_CONSTANT,
                Scalar(114.0, 114.0, 114.0)
            )
            return targetMat to (dwAligned / 2 to dhAligned / 2)
        } finally {
            matPool.release(resizedMat)
        }
    }

    private fun processTensorFlow(bitmap: Bitmap) {
        val (processedBitmap, resizeInfo) = resizeAndPadBitmap(bitmap, modelWidth, modelHeight)
        scale = resizeInfo[0]
        padX = resizeInfo[1]
        padY = resizeInfo[2]
        _tensorImage.load(processedBitmap)
        _tfImage = _tfImageProcessor.process(_tensorImage)
    }

    private fun calculateScaleRatio(srcMat: Mat): Pair<Double, Size> {
        val ratio = min(
            modelWidth.toDouble() / srcMat.cols(),
            modelHeight.toDouble() / srcMat.rows()
        ).coerceAtMost(1.0)

        val newUnpaid = Size(
            (srcMat.cols() * ratio).toInt().toDouble(),
            (srcMat.rows() * ratio).toInt().toDouble()
        )
        return ratio to newUnpaid
    }

    private fun convertMatToModelInput(mat: Mat): ByteBuffer {
        val floatArray = FloatArray(modelWidth * modelHeight * 3)
        mat.get(0, 0, floatArray)
        return bufferPool.acquire(modelWidth * modelHeight * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().put(floatArray)
            rewind()
        }
    }

    private fun resizeAndPadBitmap(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Pair<Bitmap, FloatArray> {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val scale = minOf(
            targetWidth.toFloat() / originalWidth,
            targetHeight.toFloat() / originalHeight
        ).coerceAtMost(1.0f)

        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        val offsetX = (targetWidth - scaledWidth) / 2
        val offsetY = (targetHeight - scaledHeight) / 2

        val outputBitmap = createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(GRAY_FILL, GRAY_FILL, GRAY_FILL))
        }

        Canvas(outputBitmap).apply {
            drawBitmap(
                bitmap,
                null,
                RectF(
                    offsetX.toFloat(),
                    offsetY.toFloat(),
                    offsetX + scaledWidth.toFloat(),
                    offsetY + scaledHeight.toFloat()
                ),
                null
            )
        }

        return Pair(
            outputBitmap,
            floatArrayOf(scale, offsetX.toFloat(), offsetY.toFloat())
        )
    }

    private class MatObjectPool {
        private val pool = ConcurrentLinkedQueue<Mat>()

        fun acquire(): Mat = pool.poll() ?: Mat()

        fun release(mat: Mat) {
            mat.release()
            pool.offer(mat)
        }

        fun clear() {
            pool.forEach { it.release() }
            pool.clear()
        }
    }

    private class BufferObjectPool {
        private val pool = ConcurrentLinkedQueue<ByteBuffer>()

        fun acquire(size: Int): ByteBuffer {
            return pool.poll()?.takeIf { it.capacity() >= size }?.also {
                it.clear()
            } ?: ByteBuffer.allocateDirect(size)
        }

        fun release(buffer: ByteBuffer) {
            buffer.clear()
            pool.offer(buffer)
        }

        fun clear() {
            pool.clear()
        }
    }
}