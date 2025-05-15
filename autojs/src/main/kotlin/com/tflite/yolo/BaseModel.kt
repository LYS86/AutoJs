package com.tflite.yolo

import android.graphics.Bitmap
import com.google.gson.Gson
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

    protected val threadLock = ReentrantLock()
    protected lateinit var interpreter: InterpreterApi
    private lateinit var metadataExtractor: MetadataExtractor
    private val gpuDelegate: GpuDelegate? by lazy {
        CompatibilityList().use { compatibilityList ->
            if (compatibilityList.isDelegateSupportedOnThisDevice) {
                GpuDelegate()
            } else {
                null
            }
        }
    }
    private lateinit var outputBuffer: TensorBuffer

    protected lateinit var imageProcessor: ImageProcessor

    protected var mLabels: List<String> = emptyList()
    protected lateinit var options: InterpreterApi.Options

    protected val mMetadata: ModelData by lazy {
        ModelData.ofJson(metadata)
    }
    protected lateinit var config: Config

    fun init(config: Any = Config()) {
        threadLock.lock()
        try {
            this.config = when (config) {
                is Config -> config
                is NativeObject -> Config.ofNative(config)
                is String -> Gson().fromJson(config, Config::class.java)
                else -> throw IllegalArgumentException("Unsupported config type")
            }
            loadModel(this.config.modelPath, this.config.useGpu)
            val labelsPath = this.config.labelsPath
            if (labelsPath.isNotBlank()) {
                loadLabels(labelsPath)
            }
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
     * @param isGPU 是否使用GPU
     */
    @JvmOverloads
    fun loadModel(path: String, isGPU: Boolean = true) {
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
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("${e.message}")
        } finally {
            threadLock.unlock()
        }
    }

    val metadata: String
        get() {
            if (!metadataExtractor.hasMetadata()) return ""
            return metadataExtractor.associatedFileNames.firstOrNull()
                ?.let { getAssociatedFile(it) } ?: ""
        }

    /**
     * 加载标签
     */
    fun loadLabels(path: String) {
        if (path.isBlank()) return
        try {
            mLabels = FileUtil.fromFile(path)
            mMetadata.labels = mLabels
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val labels: String
        get() = mLabels.joinToString(",")

    fun close() {
        threadLock.lock()
        try {
            if (::interpreter.isInitialized) {
                interpreter.close()
            }
            gpuDelegate?.close()
            if (::imageProcessor.isInitialized) {
                imageProcessor.release()
            }
        } finally {
            threadLock.unlock()
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
        val outputShape = interpreter.getOutputTensor(0).shape()
        val inputWidth = mMetadata.imageSize.getOrNull(0) ?: 640
        val inputHeight = mMetadata.imageSize.getOrNull(1) ?: 640
        Output.setShape(outputShape, mMetadata)
        outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)
        imageProcessor = ImageProcessor.create().size(inputWidth, inputHeight).normalize()
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

    private fun getAssociatedFile(fileName: String): String {
        return metadataExtractor.getAssociatedFile(fileName).use { stream ->
            BufferedReader(InputStreamReader(stream)).use { it.readText() }
        }
    }

    companion object {
        const val TAG = "BaseModel"
    }
}
