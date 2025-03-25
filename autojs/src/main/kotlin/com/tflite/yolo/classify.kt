package com.tflite.yolo

import android.graphics.Bitmap
import com.stardust.autojs.core.image.ImageWrapper
import org.autojs.autojs.core.yolo.BaseModel

class classify : BaseModel() {
    fun run(input: Any?): Result {
        threadLock.lock()
        try {
            val bitmap = when (input) {
                is Bitmap -> input
                is ImageWrapper -> input.bitmap
                else -> throw IllegalArgumentException("不支持的图像类型: ${input?.javaClass}")
            }

            val (tensorImage, resizeInfo) = preprocessImage(bitmap)
            val outputBuffer = runInference(tensorImage)
            val results = parseOutput(outputBuffer.floatArray, labels)

            return results
        } finally {
            threadLock.unlock()
        }
    }

    private fun parseOutput(output: FloatArray, labels: List<String>): Result {
        val results = output.mapIndexed { index, prob ->
            Result(name = labels[index], score = prob)
        }

        val topResult = results.maxByOrNull { it.score } ?: return Result(name = "", score = 0f)
        return topResult.copy(top5 = results.sortedByDescending { it.score }.take(5))
    }

    data class Result(
        val name: String,
        val score: Float,
        val top5: List<Result> = emptyList()
    ) {
        override fun toString(): String {
            return "Result(name='$name', score=$score)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Result

            if (name != other.name) return false
            if (score != other.score) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + score.hashCode()
            return result
        }

        companion object {
            fun of(name: String, score: Float): Result {
                return Result(name, score)
            }
        }
    }
}
