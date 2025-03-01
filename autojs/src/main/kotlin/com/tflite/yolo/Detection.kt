package com.tflite.yolo

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import com.stardust.autojs.core.image.ImageWrapper
import org.autojs.autojs.core.yolo.BaseModel

class Detection : BaseModel() {
    fun drawBoxes(imageWrapper: ImageWrapper, results: Array<Result>): ImageWrapper {
        return ImageWrapper.ofBitmap(FileUtil.drawBoxes(imageWrapper.bitmap, results))
    }

    fun drawBoxes(bitmap: Bitmap, results: Array<Result>): Bitmap {
        return FileUtil.drawBoxes(bitmap, results)
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
        Output.conf = 0.25F
        Output.iou = 0.7F
    }

    fun detect(imageWrapper: ImageWrapper, rect: Rect): Array<Result> {
        val croppedBitmap = cropBitmap(imageWrapper.bitmap, rect)
        val results = detect(croppedBitmap)
        croppedBitmap.recycle()
        return results.map { result ->
            Result(
                RectF(
                    result.rect.left + rect.left,
                    result.rect.top + rect.top,
                    result.rect.right + rect.left,
                    result.rect.bottom + rect.top
                ),
                result.cnf,
                result.id,
                result.name
            )
        }.toTypedArray()
    }


    fun detect(
        imageWrapper: ImageWrapper, x: Int, y: Int, width: Int, height: Int
    ): Array<Result> {
        return detect(imageWrapper, Rect(x, y, x + width, y + height))
    }

    fun detect(imageWrapper: ImageWrapper): Array<Result> {
        return detect(imageWrapper.bitmap)

    }

    fun detect(bitmap: Bitmap): Array<Result> {
        threadLock.lock()
        try {
            val image = preprocessImage(bitmap)
            val output = runInference(image)
            val results = Output.parseOutput(output.floatArray, labels)
            results.forEach { result ->
                result.rect = imageProcessor.normToOrig(result.rect)
            }
            return results
        } finally {
            threadLock.unlock()
        }
    }

    private fun cropBitmap(source: Bitmap, rect: Rect): Bitmap {
        val safeRect = Rect(
            rect.left.coerceAtLeast(0),
            rect.top.coerceAtLeast(0),
            rect.right.coerceAtMost(source.width),
            rect.bottom.coerceAtMost(source.height)
        )

        return Bitmap.createBitmap(
            source, safeRect.left, safeRect.top, safeRect.width(), safeRect.height()
        )
    }

    override fun initProcessors() {
        super.initProcessors()
        val outputShape = interpreter.getOutputTensor(0).shape()
        Output.setShape(outputShape)
    }
}