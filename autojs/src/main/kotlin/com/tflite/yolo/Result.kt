package com.tflite.yolo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

data class Result(
    var rect: RectF,
    val cnf: Float,
    val id: Int,
    val name: String
) {
    companion object {
        /**
         * 从左上角和右下角坐标创建Result（用于YOLOv10）
         * @param left 左上角x坐标
         * @param top 左上角y坐标
         * @param right 右下角x坐标
         * @param bottom 右下角y坐标
         */
        fun fromLTRB(
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

        /**
         * 从中心点坐标和宽高创建Result（用于YOLOv8/v9）
         * @param centerX 中心点x坐标
         * @param centerY 中心点y坐标
         * @param width 宽度
         * @param height 高度
         */
        fun fromXYWH(
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
    }

    /**
     * 在画布上绘制边界框
     */
    fun draw(canvas: Canvas, paint: Paint, bitmap: Bitmap) {
        canvas.drawRect(rect, paint)
    }
     fun toRect(): Rect{
         return Rect(rect.left.toInt(),rect.top.toInt(),rect.right.toInt(),rect.bottom.toInt())
     }

    override fun toString(): String {
        return "Result(rect=[%.1f, %.1f, %.1f, %.1f], cnf=%.3f, cls=%d, name='%s')".format(
            rect.left, rect.top, rect.right, rect.bottom,
            cnf, id, name
        )
    }
}
