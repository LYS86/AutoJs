package com.tflite.yolo

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.stardust.autojs.core.image.ImageWrapper

class Yolo : BaseModel() {
    @JvmOverloads
    fun detect(input: Any?, rect: Rect? = null): Array<Result> {
        threadLock.lock()
        try {
            val bitmap = when (input) {
                is Bitmap -> input
                is ImageWrapper -> input.bitmap
                else -> throw IllegalArgumentException("不支持的图像类型: ${input?.javaClass}")
            }
            val croppedBitmap = if (rect != null) cropBitmap(bitmap, rect) else null
            val image = preprocessImage(croppedBitmap ?: bitmap)
            val output = runInference(image)
            croppedBitmap?.recycle()
            val results = Output.parseOutput(output.floatArray, mMetadata)

            if (mMetadata.task == "detect") {
                results.forEach { result ->
                    result.rect = imageProcessor.normToOrig(result.rect)
                    if (rect != null) {
                        result.rect = RectF(
                            result.rect.left + rect.left,
                            result.rect.top + rect.top,
                            result.rect.right + rect.left,
                            result.rect.bottom + rect.top
                        )
                    }
                }
            }

            return results
        } finally {
            threadLock.unlock()
        }
    }

    @JvmOverloads
    fun draw(
        image: Any,
        results: Array<Result>,
        paint: Paint? = null
    ): Any {
        return when (image) {
            is Bitmap -> FileUtil.drawBoxes(image, results, paint)
            is ImageWrapper -> ImageWrapper.ofBitmap(
                FileUtil.drawBoxes(
                    image.bitmap,
                    results,
                    paint
                )
            )

            else -> throw IllegalArgumentException("不支持的图像类型")
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
}