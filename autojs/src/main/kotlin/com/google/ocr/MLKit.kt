package com.google.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.stardust.autojs.core.image.ImageWrapper

@Suppress("unused")
class MLKit {
    private lateinit var recognizer: com.google.mlkit.vision.text.TextRecognizer
    private var isLoaded = false

    /**
     * 初始化OCR识别器
     * @param language 语言代码，默认为拉丁字母，"zh"表示中文
     */
    fun init(language: String = "") {
        if (isLoaded) recycle()
        val options = when (language) {
            "zh" -> ChineseTextRecognizerOptions.Builder().build()
            else -> TextRecognizerOptions.Builder().build()
        }
        recognizer = TextRecognition.getClient(options)
        isLoaded = true
    }

    fun detect(image: ImageWrapper): Results {
        return detect(image.bitmap)
    }

    /**
     * 执行OCR识别
     * @param bitmap 要识别的位图
     * @return Results对象包含识别结果
     */
    fun detect(bitmap: Bitmap): Results {
        if (!isLoaded) init()
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            val visionText = Tasks.await(recognizer.process(image))
            Results(visionText)
        } catch (e: Exception) {
            Results(error = e.message ?: "未知错误")
        }
    }

    /**
     * 执行局部区域OCR识别
     * @param bitmap 要识别的位图
     * @param rect 识别区域矩形
     * @return Results对象包含识别结果
     */
    fun detect(bitmap: Bitmap, rect: Rect): Results {
        if (!isLoaded) init()

        if (rect.isEmpty || !Rect(0, 0, bitmap.width, bitmap.height).contains(rect)) {
            return Results(error = "无效的识别区域: $rect,图片尺寸: ${bitmap.width}x${bitmap.height}")
        }

        val croppedBitmap = try {
            Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
        } catch (e: Exception) {
            return Results(error = "区域裁剪失败: ${e.message}")
        }

        val results = detect(croppedBitmap)
        croppedBitmap.recycle()
        if (results.hasError()) return results
        return  Results(results.raw, rect, results.error)
    }

    /**
     * 执行局部区域OCR识别(ImageWrapper版本)
     * @param image 要识别的图像
     * @param rect 识别区域矩形
     * @return Results对象包含识别结果
     */
    fun detect(image: ImageWrapper, rect: Rect): Results {
        return detect(image.bitmap, rect)
    }

    /**
     * 释放识别器资源
     */
    fun recycle() {
        if (!isLoaded) return
        recognizer.close()
        isLoaded = false
    }

    /**
     * OCR识别结果封装类
     * @param raw 原始识别结果
     * @param regionRect 识别区域矩形(用于局部识别时坐标映射)
     * @param error 错误信息
     */
    class Results(
        val raw: Text? = null,
        private val regionRect: Rect? = null,
        val error: String? = null
    ) {
        /**
         * 获取全部识别文本
         */
        val text: String = raw?.text ?: ""

        /**
         * 检查是否有错误
         */
        fun hasError() = error != null

        /**
         * 检查是否为空结果
         */
        fun isEmpty() = text.isEmpty()

        /**
         * 获取原始识别数据(不进行坐标映射)
         */
        fun rawText(): Text? = raw

        /**
         * 获取所有文本块(段落级别)转换为Result数组
         */
        fun blocks(): Array<Result> {
            return raw?.textBlocks?.map { block ->
                Result(
                    text = block.text,
                    rect = mapRect(block.boundingBox),
                    conf = 0f
                )
            }?.toTypedArray() ?: emptyArray()
        }

        /**
         * 获取所有文本行转换为Result数组
         */
        fun lines(): Array<Result> {
            return raw?.textBlocks?.flatMap { block ->
                block.lines.map { line ->
                    Result(
                        text = line.text,
                        rect = mapRect(line.boundingBox),
                        conf = line.confidence
                    )
                }
            }?.toTypedArray() ?: emptyArray()
        }

        /**
         * 获取所有文本元素(单词/字符级别)转换为Result数组
         */
        fun elements(): Array<Result> {
            return raw?.textBlocks?.flatMap { block ->
                block.lines.flatMap { line ->
                    line.elements.map { element ->
                        Result(
                            text = element.text,
                            rect = mapRect(element.boundingBox),
                            conf = element.confidence
                        )
                    }
                }
            }?.toTypedArray() ?: emptyArray()
        }

        /**
         * 将识别结果的坐标映射回原图坐标
         */
        private fun mapRect(rect: Rect?): Rect {
            if (rect == null) return Rect()
            return when (regionRect) {
                null -> rect
                else -> Rect(
                    rect.left + regionRect.left,
                    rect.top + regionRect.top,
                    rect.right + regionRect.left,
                    rect.bottom + regionRect.top
                )
            }
        }
    }

    /**
     * 单条识别结果
     */
    data class Result(
        val text: String,
        val rect: Rect,
        val conf: Float,
    ) {
        companion object {
            /**
             * 创建一个空结果
             */
            fun empty() = Result("", Rect(), 0f)
        }

        /**
         * 检查是否为空结果
         */
        fun isEmpty() = text.isEmpty()

        override fun toString(): String {
            return "Result(text='$text', rect=$rect, conf=$conf)"
        }
    }
}