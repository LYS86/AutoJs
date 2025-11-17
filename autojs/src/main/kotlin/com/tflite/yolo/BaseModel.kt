package com.tflite.yolo

import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.google.gson.Gson
import org.mozilla.javascript.NativeObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.metadata.MetadataExtractor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

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
            loadModel(this.config.modelPath, createOptions(useGpu = this.config.useGpu))
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
    fun loadModel(path: String) {
        options = createOptions()
        loadModel(path, options)
    }

    /**
     * 加载模型（使用指定选项）
     */
    fun loadModel(path: String, options: InterpreterApi.Options) {
        threadLock.lock()
        try {
            Timber.d("加载模型：$path")
            val modelBuffer: MappedByteBuffer = FileUtil.loadModel(path)
            metadataExtractor = MetadataExtractor(modelBuffer)
            interpreter = createInterpreterApi(modelBuffer, options)
            initProcessors()
            Timber.d("模型加载完成")
        } catch (e: Exception) {
            Timber.e(e)
            throw RuntimeException("${e.message}")
        } finally {
            threadLock.unlock()
        }
    }


    private fun createInterpreterApi(
        modelBuffer: MappedByteBuffer,
        options: InterpreterApi.Options
    ): InterpreterApi {
        val strategies = listOf(
            options,
            createOptions(useGpu = false),
            createOptions(useGpu = false, useNNAPI = false),
            createOptions(useGpu = false, useNNAPI = false, useXNNPACK = false)
        )

        strategies.forEachIndexed { index, options ->
            Timber.d(
                "Interpreter配置[#%d]: numThreads=%d, useNNAPI=%b, useXNNPACK=%b, delegates=%s",
                index + 1,
                options.numThreads,
                options.useNNAPI,
                options.useXNNPACK,
                options.getDelegates()
            )

            runCatching {
                return InterpreterApi.create(modelBuffer, options)
            }.onFailure { error ->
                if (index == strategies.lastIndex) throw error
                Timber.w(error, "加载失败，降级处理")
            }
        }

        throw IllegalStateException("All fallback strategies exhausted")
    }

    val metadata: String
        get() {
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
            Timber.e(e)
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


    fun createOptions(
        useGpu: Boolean = true,
        useNNAPI: Boolean = true,
        useXNNPACK: Boolean = true
    ): InterpreterApi.Options {
        val options = InterpreterApi.Options().apply {
            this.useNNAPI = useNNAPI
            this.useXNNPACK = useXNNPACK
        }
        if (useGpu && gpuDelegate != null) {
            options.addDelegate(gpuDelegate)
        }
        return options
    }

    protected open fun initProcessors() {
        val outputShape = interpreter.getOutputTensor(0).shape()
        val inputWidth = mMetadata.imageSize[0]
        val inputHeight = mMetadata.imageSize[1]
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
            readMetadataFile(stream)
        }
    }

    private fun readMetadataFile(stream: InputStream): String {
        val data = stream.readBytes()
        return try {
            val inflated = InflaterInputStream(data.inputStream(), Inflater(true)).use {
                it.readBytes()
            }
            String(inflated, Charsets.UTF_8)
        } catch (_: Exception) {
            String(data, Charsets.UTF_8)
        }
    }

}
