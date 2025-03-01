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
import org.tensorflow.lite.support.image.ImageProcessor  as TfImageProcessor

class ImageProcessor private constructor() {

    enum class Mode {
        OPENCV,
        TENSORFLOW
    }

    internal var modelWidth = 640
    internal var modelHeight = 640
    private var normalizeMean = 0f
    private var normalizeStd = 255f
    private var mode = Mode.OPENCV
    private var stride = 32

    private val matPool = MatObjectPool()
    private val bufferPool = BufferObjectPool()

    private var _opencvBuffer: ByteBuffer = ByteBuffer.allocateDirect(0)
    private var _opencvBitmap: Bitmap = createBitmap(1, 1)
    private val _tensorImage = TensorImage(DataType.FLOAT32)
    private val _tfImageProcessor = TfImageProcessor.Builder()
        .add(NormalizeOp(normalizeMean, normalizeStd))
        .build()
    private var _tfImage: TensorImage = TensorImage(DataType.FLOAT32)

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
        this.mode  = if (mode == Mode.OPENCV && !opencvAvailable) Mode.TENSORFLOW else mode
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
            normRect.left  * modelWidth,
            normRect.top  * modelHeight,
            normRect.right  * modelWidth,
            normRect.bottom  * modelHeight
        )
        return absToOrig(absRect)
    }

    fun absToOrig(absRect: RectF): RectF {
        return RectF(
            (absRect.left  - padX).coerceAtLeast(0f) / scale,
            (absRect.top  - padY).coerceAtLeast(0f) / scale,
            (absRect.right  - padX).coerceAtMost(modelWidth.toFloat())  / scale,
            (absRect.bottom  - padY).coerceAtMost(modelHeight.toFloat())  / scale
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
            Utils.bitmapToMat(bitmap,  srcMat)
            Imgproc.cvtColor(srcMat,  srcMat, Imgproc.COLOR_RGBA2RGB)

            val (r, newUnpaid) = calculateScaleRatio(srcMat)
            scale = r.toFloat()

            val (usedPaddedMat, padding) = padImage(srcMat, newUnpaid, paddedMat)
            padX = padding.first.toFloat()
            padY = padding.second.toFloat()

            usedPaddedMat.convertTo(normalizedMat,  CvType.CV_32FC3, 1.0 / 255.0)
            _opencvBuffer = convertMatToModelInput(normalizedMat)

            val tempMat = matPool.acquire()
            try {
                normalizedMat.convertTo(tempMat,  CvType.CV_8UC3, 255.0)
                Imgproc.cvtColor(tempMat,  tempMat, Imgproc.COLOR_RGB2RGBA)
                _opencvBitmap = createBitmap(tempMat.cols(),  tempMat.rows(),  Bitmap.Config.ARGB_8888).apply {
                    Utils.matToBitmap(tempMat,  this)
                }
            } finally {
                matPool.release(tempMat)
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
            Imgproc.resize(srcMat,  resizedMat, newUnpaid)

            val targetWidth = modelWidth
            val targetHeight = modelHeight
            val currentWidth = resizedMat.cols()
            val currentHeight = resizedMat.rows()

            val paddingWidth = targetWidth - currentWidth
            val paddingHeight = targetHeight - currentHeight

            val paddingLeft = (paddingWidth / 2 / stride) * stride
            val paddingRight = paddingWidth - paddingLeft
            val paddingTop = (paddingHeight / 2 / stride) * stride
            val paddingBottom = paddingHeight - paddingTop

            Core.copyMakeBorder(
                resizedMat, targetMat,
                paddingTop, paddingBottom,
                paddingLeft, paddingRight,
                Core.BORDER_CONSTANT,
                Scalar(114.0, 114.0, 114.0)
            )

            return targetMat to (paddingLeft to paddingTop)
        } finally {
            matPool.release(resizedMat)
        }
    }

    fun sliceImage(original: Bitmap): List<Pair<Bitmap, RectF>> {
        val originalWidth = original.width
        val originalHeight = original.height
        val cols = kotlin.math.ceil(originalWidth.toFloat()  / modelWidth).toInt()
        val rows = kotlin.math.ceil(originalHeight.toFloat()  / modelHeight).toInt()

        return when (mode) {
            Mode.OPENCV -> sliceWithOpenCV(original, rows, cols)
            Mode.TENSORFLOW -> sliceWithAndroid(original, rows, cols)
        }
    }

    private fun sliceWithOpenCV(original: Bitmap, rows: Int, cols: Int): List<Pair<Bitmap, RectF>> {
        val slices = mutableListOf<Pair<Bitmap, RectF>>()
        val srcMat = Mat()
        Utils.bitmapToMat(original,  srcMat)

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val startX = j * modelWidth
                val startY = i * modelHeight
                val endX = (startX + modelWidth).coerceAtMost(srcMat.cols())
                val endY = (startY + modelHeight).coerceAtMost(srcMat.rows())

                if (endX > startX && endY > startY) {
                    val roi = Mat(srcMat, org.opencv.core.Rect(startX,  startY, endX - startX, endY - startY))
                    try {
                        val sliceBitmap = createBitmap(roi.width(),  roi.height())
                        Utils.matToBitmap(roi,  sliceBitmap)
                        slices.add(Pair(
                            sliceBitmap,
                            RectF(startX.toFloat(),  startY.toFloat(),  endX.toFloat(),  endY.toFloat())
                        ))
                    } finally {
                        roi.release()
                    }
                }
            }
        }
        srcMat.release()
        return slices
    }

    private fun sliceWithAndroid(original: Bitmap, rows: Int, cols: Int): List<Pair<Bitmap, RectF>> {
        val slices = mutableListOf<Pair<Bitmap, RectF>>()

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val startX = j * modelWidth
                val startY = i * modelHeight
                val endX = (startX + modelWidth).coerceAtMost(original.width)
                val endY = (startY + modelHeight).coerceAtMost(original.height)

                if (endX > startX && endY > startY) {
                    val sliceBitmap = Bitmap.createBitmap(
                        original,
                        startX,
                        startY,
                        endX - startX,
                        endY - startY
                    )
                    slices.add(Pair(
                        sliceBitmap,
                        RectF(startX.toFloat(),  startY.toFloat(),  endX.toFloat(),  endY.toFloat())
                    ))
                }
            }
        }
        return slices
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
            modelWidth.toDouble()  / srcMat.cols(),
            modelHeight.toDouble()  / srcMat.rows()
        ).coerceAtMost(1.0)

        val newUnpaid = Size(
            (srcMat.cols()  * ratio).toInt().toDouble(),
            (srcMat.rows()  * ratio).toInt().toDouble()
        )
        return ratio to newUnpaid
    }

    private fun convertMatToModelInput(mat: Mat): ByteBuffer {
        val floatArray = FloatArray(modelWidth * modelHeight * 3)
        mat.get(0,  0, floatArray)
        return bufferPool.acquire(modelWidth  * modelHeight * 3 * 4).apply {
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
            targetWidth.toFloat()  / originalWidth,
            targetHeight.toFloat()  / originalHeight
        ).coerceAtMost(1.0f)

        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        val offsetX = (targetWidth - scaledWidth) / 2
        val offsetY = (targetHeight - scaledHeight) / 2

        val outputBitmap = createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(GRAY_FILL,  GRAY_FILL, GRAY_FILL))
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
            floatArrayOf(scale, offsetX.toFloat(),  offsetY.toFloat())
        )
    }

    private class MatObjectPool {
        private val pool = ConcurrentLinkedQueue<Mat>()

        fun acquire(): Mat = pool.poll()  ?: Mat()

        fun release(mat: Mat) {
            mat.release()
            pool.offer(mat)
        }

        fun clear() {
            pool.forEach  { it.release()  }
            pool.clear()
        }
    }

    private class BufferObjectPool {
        private val pool = ConcurrentLinkedQueue<ByteBuffer>()

        fun acquire(size: Int): ByteBuffer {
            return pool.poll()?.takeIf  { it.capacity()  >= size }?.also {
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