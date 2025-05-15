package com.tflite.yolo

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Result(
    @SerializedName("rect") var rect: RectF = RectF(),
    @SerializedName("cnf") val cnf: Float = 0f,
    @SerializedName("id") val id: Int = -1,
    @SerializedName("name") val name: String = "",
    @SerializedName("results") val allResults: List<Result> = emptyList()
) {
    companion object {
        fun ofLTRB(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            cnf: Float,
            cls: Int,
            clsName: String
        ): Result {
            return Result(RectF(left, top, right, bottom), cnf, cls, clsName)
        }

        fun ofXYWH(
            centerX: Float,
            centerY: Float,
            width: Float,
            height: Float,
            cnf: Float,
            cls: Int,
            clsName: String
        ): Result {
            val left = centerX - (width / 2f)
            val top = centerY - (height / 2f)
            val right = centerX + (width / 2f)
            val bottom = centerY + (height / 2f)
            return Result(RectF(left, top, right, bottom), cnf, cls, clsName)
        }

        fun ofClassify(
            name: String,
            cnf: Float,
            id: Int = -1,
            allResults: List<Result> = emptyList()
        ): Result {
            return Result(cnf = cnf, id = id, name = name, allResults = allResults)
        }

        fun ofJson(json: String): Result {
            if (json.isBlank()) return Result()
            try {
                return Gson().fromJson(json, Result::class.java) ?: Result()
            } catch (e: Exception) {
                e.printStackTrace()
                return Result()
            }
        }
    }

    fun draw(canvas: Canvas, paint: Paint) {
        if (!rect.isEmpty) {
            canvas.drawRect(rect, paint)
        }
    }

    fun toRect(): Rect {
        return Rect(
            rect.left.toInt(),
            rect.top.toInt(),
            rect.right.toInt(),
            rect.bottom.toInt()
        )
    }

    fun top(n: Int = 1): List<Result> {
        return if (allResults.isNotEmpty()) {
            allResults.sortedByDescending { it.cnf }.take(n)
        } else {
            listOf(this)
        }
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }

    override fun toString(): String {
        return this.toJson()
    }
}