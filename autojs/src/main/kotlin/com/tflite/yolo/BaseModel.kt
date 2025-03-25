package org.autojs.autojs.core.yolo

import android.graphics.Bitmap
import com.tflite.yolo.FileUtil
import org.mozilla.javascript.NativeObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.metadata.MetadataExtractor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.util.concurrent.locks.ReentrantLock

abstract class BaseModel {

    // 线程保护
    protected val threadLock = ReentrantLock()

    // TFLite组件
    protected lateinit var interpreter: InterpreterApi
    protected lateinit var metadataExtractor: MetadataExtractor
    protected var gpuDelegate: GpuDelegate? = null
    protected lateinit var outputBuffer: TensorBuffer

    // 图像处理
    protected var inputWidth = 640
    protected var inputHeight = 640
    protected lateinit var imageProcessor: ImageProcessor
    protected lateinit var tensorImage: TensorImage

    // 模型数据
    protected var labels: List<String> = emptyList()
    protected lateinit var options: InterpreterApi.Options

    init {
        initGpu()
    }

    fun init(options: NativeObject) {
        threadLock.lock()
        try {
            val model = options.get("model") as? String
            val labels = options.get("labels") as? String
            val isGPU = options.get("gpu") as? Boolean ?: false
            if (model.isNullOrBlank()) {
                throw IllegalArgumentException("模型路径不能为空")
            }
            loadModel(model, isGPU)
            loadLabels(labels)
        } finally {
            threadLock.unlock()
        }
    }

    /**
     * 获取最后一次推理的运行时间（毫秒）
     */
    fun runtime(): Long {
        return interpreter.lastNativeInferenceDurationNanoseconds / 1_000_000
    }

    /**
     * 加载模型
     * @param path 模型文件路径
     */
    fun loadModel(path: String) {
        loadModel(path, true)
    }

    /**
     * 加载模型
     * @param path 模型文件路径
     * @param isGPU 是否使用GPU
     */
    fun loadModel(path: String, isGPU: Boolean) {
        options = getOptions(isGPU)
        loadModel(path, options)
    }

    /**
     * 加载模型（使用指定选项）
     */
    fun loadModel(path: String, options: InterpreterApi.Options) {
        threadLock.lock()
        try {
            val modelBuffer: MappedByteBuffer = FileUtil.loadModel(path)
            metadataExtractor = MetadataExtractor(modelBuffer)
            interpreter = InterpreterApi.create(modelBuffer, options)
            initProcessors()
            loadLabels(null)
        } catch (e: Exception) {
            throw RuntimeException("${e.message}")
        } finally {
            threadLock.unlock()
        }
    }

    /**
     * 获取模型元数据
     */
    fun getMetadata(): String {
        if (!hasMetadata()) return ""
        return metadataExtractor.associatedFileNames.firstOrNull()?.let { getAssociatedFile(it) }
            ?: ""
    }

    /**
     * 加载标签
     */
    fun loadLabels(path: String?) {
        if (!path.isNullOrBlank()) {
            labels = FileUtil.fromFile(path)
            return
        }
        val info = getMetadata()
        if (info.isBlank()) return
        labels = FileUtil.fromJson(info)
    }

    /**
     * 获取标签列表
     */
    fun getLabels(): Array<String> {
        return labels.toTypedArray()
    }

    fun close() {
        threadLock.lock()
        try {
            if (::interpreter.isInitialized) {
                interpreter.close()
            }
            gpuDelegate?.close()
            gpuDelegate = null
        } finally {
            threadLock.unlock()
        }
    }

    /**
     * 缩放图像并保持宽高比，同时填充以匹配目标尺寸。
     *
     * @param bitmap 原始图像
     * @param targetWidth 模型期望的宽度
     * @param targetHeight 模型期望的高度
     * @return 经过缩放和填充后的图像，尺寸严格匹配 targetWidth 和 targetHeight
     */
    protected fun resizeAndPadBitmap(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Pair<Bitmap, FloatArray> {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val scale =
            minOf(targetWidth.toFloat() / originalWidth, targetHeight.toFloat() / originalHeight)
        val scaledWidth = (originalWidth * scale).toInt()
        val scaledHeight = (originalHeight * scale).toInt()
        val offsetX = (targetWidth - scaledWidth) / 2f
        val offsetY = (targetHeight - scaledHeight) / 2f
        val outputBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(outputBitmap).apply {
            drawBitmap(
                bitmap,
                null,
                android.graphics.RectF(
                    offsetX,
                    offsetY,
                    offsetX + scaledWidth,
                    offsetY + scaledHeight
                ),
                null
            )
        }
        return Pair(outputBitmap, floatArrayOf(scale, offsetX, offsetY))
    }

    private fun initGpu() {
        CompatibilityList().use { compatibilityList ->
            if (compatibilityList.isDelegateSupportedOnThisDevice) {
                gpuDelegate = GpuDelegate()
            }
        }
    }

    private fun getOptions(isGPU: Boolean): InterpreterApi.Options {
        val opts = InterpreterApi.Options().apply {
            useNNAPI = true
        }
        if (!isGPU) return opts
        if (gpuDelegate == null) {
            return opts
        }
        return opts.apply { addDelegate(gpuDelegate) }
    }

    protected open fun initProcessors() {
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()
        inputWidth = inputShape[1]
        inputHeight = inputShape[2]
        outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)
        imageProcessor = ImageProcessor.Builder().add(NormalizeOp(0f, 255f)).build()
        tensorImage = TensorImage(DataType.FLOAT32)
    }

    protected fun preprocessImage(bitmap: Bitmap): Pair<TensorImage, FloatArray> {
        val (resizedBitmap, resizeInfo) = resizeAndPadBitmap(bitmap, inputWidth, inputHeight)
        tensorImage.load(resizedBitmap)
        return Pair(imageProcessor.process(tensorImage), resizeInfo)
    }

    protected fun runInference(tensorImage: TensorImage): TensorBuffer {
        interpreter.run(tensorImage.buffer, outputBuffer.buffer)
        return outputBuffer
    }

    private fun hasMetadata(): Boolean = metadataExtractor.hasMetadata()

    private fun getAssociatedFile(fileName: String): String {
        return metadataExtractor.getAssociatedFile(fileName).use { stream ->
            BufferedReader(InputStreamReader(stream)).use { it.readText() }
        }
    }

    companion object {
        const val TAG = "BaseModel"
    }
}
