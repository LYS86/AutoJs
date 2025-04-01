package com.lin.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.stardust.autojs.core.image.ImageWrapper

class MLKit {
    private lateinit var recognizer: com.google.mlkit.vision.text.TextRecognizer
    private var isLoaded = false

    /**
     * 初始化OCR识别器
     * @param language 语言代码，默认为空(自动检测)，"zh"表示中文
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
        val bitmap = image.bitmap
        return detect(bitmap)
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
     * 释放识别器资源
     */
    fun recycle() {
        if (!isLoaded) return
        recognizer.close()
        isLoaded = false
    }

    /**
     * OCR识别结果封装类
     */
    class Results(
        val raw: Text? = null,
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
         * 获取原始识别数据
         */
        fun rawText(): Text? = raw

        /**
         * 获取所有文本块(段落级别)转换为Result数组
         */
        fun blocks(): Array<Result> {
            return raw?.textBlocks?.map { block ->
                Result(
                    text = block.text,
                    rect = block.boundingBox ?: Rect(),
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
                        rect = line.boundingBox ?: Rect(),
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
                            rect = element.boundingBox ?: Rect(),
                            conf = element.confidence
                        )
                    }
                }
            }?.toTypedArray() ?: emptyArray()
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