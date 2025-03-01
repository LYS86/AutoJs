package org.autojs.autojs.core.yolo

import android.graphics.Bitmap
import com.stardust.autojs.core.image.ImageWrapper
import com.tflite.yolo.FileUtil
import com.tflite.yolo.ImageProcessor
import org.mozilla.javascript.NativeObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.metadata.MetadataExtractor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.util.concurrent.locks.ReentrantLock

abstract class BaseModel {

    // 线程保护
    protected val threadLock = ReentrantLock()

    // TFLite组件
    protected lateinit var interpreter: InterpreterApi
    private lateinit var metadataExtractor: MetadataExtractor
    private var gpuDelegate: GpuDelegate? = null
    private lateinit var outputBuffer: TensorBuffer

    // 图像处理
    protected var inputWidth = 640
    protected var inputHeight = 640
    protected lateinit var imageProcessor: ImageProcessor

    // 模型数据
    protected var labels: List<String> = emptyList()
    protected lateinit var options: InterpreterApi.Options

    init {
        initGpu()
    }

    fun init(options: NativeObject) {
        threadLock.lock()
        try {
            val model = options["model"] as? String
            val labels = options["labels"] as? String
            val isGPU = options["gpu"] as? Boolean == true
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
            if (::imageProcessor.isInitialized) {
                imageProcessor.release()
            }
        } finally {
            threadLock.unlock()
        }
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
        imageProcessor = ImageProcessor.create()
            .size(inputWidth, inputHeight)
            .normalize()
            .mode(ImageProcessor.Mode.OPENCV)
    }

    protected fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val inputBuffer = imageProcessor.ofBitmap(bitmap)
        return inputBuffer.buffer
    }

    protected fun runInference(image: ByteBuffer): TensorBuffer {
        interpreter.run(image, outputBuffer.buffer)
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
