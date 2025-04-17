package com.tflite.yolo

import android.graphics.Bitmap
import android.util.Log
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
        Output.conf = 0.25F
        Output.iou = 0.7F
    }


    /**
     * 使用切片方式进行目标检测
     * @param bitmap 输入图片
     * @return 检测结果数组
     */

    fun detect_test(
        input: Any?,
    ): Array<Result> {
        threadLock.lock()
        try {
            val bitmap = when (input) {
                is Bitmap -> input
                is ImageWrapper -> input.bitmap
                else -> throw IllegalArgumentException("不支持的图像类型: ${input?.javaClass}")
            }
            Log.d(TAG, "开始切片检测...")
            Log.d(TAG, "输入尺寸: ${bitmap.width}x${bitmap.height}")
            Log.d(
                TAG,
                "模型尺寸: ${imageProcessor.modelWidth}x${imageProcessor.modelHeight}"
            )

            // 切片图片
            val tiles = imageProcessor.sliceImage(bitmap)
            val allResults = mutableListOf<Result>()
            tiles.forEachIndexed { index, (tile, tileRect) ->
                Log.d(
                    TAG,
                    "处理切片 ${index + 1}/${tiles.size} [位置: (${tileRect.left}, ${tileRect.top})]"
                )
                ImageWrapper.ofBitmap(tile).saveTo("/sdcard/js/tile_${index + 1}.png")
            }
            // 处理每个切片
            tiles.forEachIndexed { index, (tile, tileRect) ->
                Log.d(
                    TAG,
                    "处理切片 ${index + 1}/${tiles.size} [位置: (${tileRect.left}, ${tileRect.top})]"
                )

                try {
                    // 预处理切片
                    imageProcessor.ofBitmap(tile)
                    imageProcessor.save("/sdcard/js/tile_${index + 1}_processed.png")

                    val output = runInference(imageProcessor.buffer)

                    // 解析结果并转换坐标
                    val results = Output.parseOutput(output.floatArray, labels).also {
                        Log.d(TAG, "切片 ${index + 1} 检测到 ${it.size} 个目标")
                    }

                    // 将切片坐标转换为原图坐标
                    results.forEach { result ->
                        // 先将模型输出归一化坐标转换为切片坐标
                        val sliceRect = imageProcessor.normToOrig(result.rect)
                        // 再将切片坐标转换为原图坐标
                        result.rect.set(
                            sliceRect.left + tileRect.left,
                            sliceRect.top + tileRect.top,
                            sliceRect.right + tileRect.left,
                            sliceRect.bottom + tileRect.top
                        )
                    }

                    allResults.addAll(results)
                } finally {
                    // 确保回收临时创建的bitmap
                    tile.recycle()
                }
            }

            Log.d(TAG, "检测完成，共检测到 ${allResults.size} 个目标")
            return allResults.toTypedArray()
        } finally {
            threadLock.unlock()
        }
    }

    fun detect(input: Any?): Array<Result> {
        threadLock.lock()
        try {
            val bitmap = when (input) {
                is Bitmap -> input
                is ImageWrapper -> input.bitmap
                else -> throw IllegalArgumentException("不支持的图像类型: ${input?.javaClass}")
            }
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

    override fun initProcessors() {
        super.initProcessors()
        val outputShape = interpreter.getOutputTensor(0).shape()
        Output.setShape(outputShape)
    }
}