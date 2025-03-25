package com.tflite.yolo

import android.graphics.Bitmap
import com.stardust.autojs.core.image.ImageWrapper
import org.autojs.autojs.core.yolo.BaseModel

class Detection : BaseModel() {
    fun drawBoxes(bitmap: Any, results: Array<Result>): Any {
        return when (bitmap) {
            is Bitmap -> FileUtil.drawBoxes(bitmap, results)
            is ImageWrapper -> ImageWrapper.ofBitmap(FileUtil.drawBoxes(bitmap.bitmap, results))
            else -> throw IllegalArgumentException("不支持的图像类型: ${bitmap.javaClass}")
        }
    }

    /**
     * 设置检测阈值
     * @param conf 置信度阈值 (0-1)
     * @param iou IoU阈值 (0-1)
     */
    fun setThresholds(conf: Float? = null, iou: Float? = null) {
        conf?.let {
            require(it in 0.0F..1.0F) { "置信度阈值必须在0到1之间" }
            Output.conf = it
        }
        iou?.let {
            require(it in 0.0F..1.0F) { "IoU阈值必须在0到1之间" }
            Output.iou = it
        }
    }

    /**
     * 重置检测阈值为默认值
     */
    fun resetThresholds() {
        Output.conf = 0.3F
        Output.iou = 0.7F
    }

    fun run(input: Any?): Array<Result> {
        threadLock.lock()
        try {
            val bitmap = when (input) {
                is Bitmap -> input
                is ImageWrapper -> input.bitmap
                else -> throw IllegalArgumentException("不支持的图像类型: ${input?.javaClass}")
            }

            val (tensorImage, resizeInfo) = time(TAG, "图片预处理") { preprocessImage(bitmap) }
            val outputBuffer = time(TAG, "推理") { runInference(tensorImage) }
            val results = Output.parseOutput(outputBuffer.floatArray, labels)

            val scale = resizeInfo[0]
            val offsetX = resizeInfo[1]
            val offsetY = resizeInfo[2]

            results.forEach { result ->
                result.rect.apply {
                    left =
                        ((left * inputWidth - offsetX) / scale).coerceIn(0f, bitmap.width.toFloat())
                    top = ((top * inputHeight - offsetY) / scale).coerceIn(
                        0f,
                        bitmap.height.toFloat()
                    )
                    right = ((right * inputWidth - offsetX) / scale).coerceIn(
                        0f,
                        bitmap.width.toFloat()
                    )
                    bottom = ((bottom * inputHeight - offsetY) / scale).coerceIn(
                        0f,
                        bitmap.height.toFloat()
                    )
                }
            }

            return results
        } finally {
            threadLock.unlock()
        }
    }

    override fun initProcessors() {
        super.initProcessors()
        val outputShape = interpreter.getOutputTensor(0).shape()
        Output.setShape(outputShape)
    }
}