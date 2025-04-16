package com.tflite.yolo

import android.graphics.RectF
import android.util.Log

object Output {
    var conf = 0.25F // 默认置信度阈值
    var iou = 0.7F // 默认IOU阈值
    var numDetections = 0 // 预测框数量（如 8400）
    var numClasses = 0 // 类别数量（如 84 - 4 = 80）

    /**
     * 解析YOLO模型的输出
     * @param outputArray 模型输出的浮点数组
     * @param labels 类别标签列表
     * @return 经过NMS处理后的检测结果数组
     */
    fun parseOutput(outputArray: FloatArray, labels: List<String>): Array<Result> {
        return (if (numDetections == 300) yolo10(outputArray, labels) else yolo(
            outputArray,
            labels
        )).toTypedArray()
    }

    fun setShape(shape: IntArray) {
        when {
            shape[1] == 300 && shape[2] == 6 -> { // YOLOv10
                Log.d("Output","输出形状：${shape.contentToString()}")
                numDetections = shape[1]
            }
            else -> { // YOLOv8, YOLOv9, YOLOv11
                Log.d("Output","输出形状：${shape.contentToString()}")
                numClasses = shape[1] - 4
                numDetections = shape[2]
            }

        }
    }

    /**
     * 处理YOLOv10模型的输出
     * @param outputArray 模型输出的浮点数组，每个检测框包含6个值[x1,y1,x2,y2,score,class_id]
     * @param labels 类别标签列表
     * @return 经过NMS处理后的检测结果数组
     */
    private fun yolo10(outputArray: FloatArray, labels: List<String>): List<Result> {
        val results = ArrayList<Result>()
        val numDet = numDetections

        var i = 0
        while (i < numDet) {
            val score = outputArray[i * 6 + 4]
            if (score >= conf) {
                results.add(
                    Result.fromLTRB(
                        outputArray[i * 6],     // left
                        outputArray[i * 6 + 1], // top
                        outputArray[i * 6 + 2], // right
                        outputArray[i * 6 + 3], // bottom
                        score,
                        outputArray[i * 6 + 5].toInt(),
                        labels.getOrElse(outputArray[i * 6 + 5].toInt()) { "unknown" }
                    ))
            }
            i++
        }
        return applyNMS(results)
    }

    /**
     * 处理YOLOv8/v9/v11模型的输出
     * @param outputArray 模型输出的浮点数组
     * @param labels 类别标签列表
     * @return 经过NMS处理后的检测结果数组
     */
    private fun yolo(outputArray: FloatArray, labels: List<String>): List<Result> {
        val results = ArrayList<Result>()
        val numDet = numDetections
        val numCls = numClasses
        val offsets = IntArray(numCls + 4) { i ->
            when (i) {
                0 -> 0
                1 -> numDet
                2 -> numDet * 2
                3 -> numDet * 3
                else -> numDet * (4 + i - 4)
            }
        }

        var i = 0
        while (i < numDet) {
            val baseOffset = i + numDet * 4
            var maxScore = 0f
            var cls = 0
            var j = 0
            while (j < numCls) {
                val score = outputArray[baseOffset + j * numDet]
                if (score > maxScore) {
                    maxScore = score
                    cls = j
                }
                j++
            }

            if (maxScore >= conf) {
                results.add(
                    Result.fromXYWH(
                        outputArray[i + offsets[0]],
                        outputArray[i + offsets[1]],
                        outputArray[i + offsets[2]],
                        outputArray[i + offsets[3]],
                        maxScore,
                        cls,
                        labels.getOrElse(cls) { "unknown" }
                    ))
            }
            i++
        }
        return applyNMS(results)
    }

    /**
     * 应用非极大值抑制（NMS）处理
     * @param results 检测结果数组
     * @return 经过NMS处理后的检测结果数组
     */
    private fun applyNMS(results: List<Result>): List<Result> {
        if (results.isEmpty()) return results
        val resultsList = results.toMutableList()
        resultsList.sortWith { r1, r2 -> (r2.cnf - r1.cnf).toInt() }
        val keep = BooleanArray(results.size) { true }

        for (i in results.indices) {
            if (!keep[i]) continue
            val r1 = results[i]
            for (j in i + 1 until results.size) {
                if (!keep[j]) continue
                val r2 = results[j]
                if (r1.id == r2.id && calculateIoU(r1.rect, r2.rect) > iou) {
                    keep[j] = false
                }
            }
        }
        return resultsList.filterIndexed { index, _ -> keep[index] }
    }

    /**
     * 计算两个矩形的交并比（IoU）
     * @param rect1 第一个矩形
     * @param rect2 第二个矩形
     * @return 两个矩形的交并比
     */
    private fun calculateIoU(rect1: RectF, rect2: RectF): Float {
        val intersectLeft = maxOf(rect1.left, rect2.left)
        val intersectTop = maxOf(rect1.top, rect2.top)
        val intersectRight = minOf(rect1.right, rect2.right)
        val intersectBottom = minOf(rect1.bottom, rect2.bottom)

        if (intersectLeft >= intersectRight || intersectTop >= intersectBottom) return 0f

        val intersectArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop)
        val area1 = rect1.width() * rect1.height()
        val area2 = rect2.width() * rect2.height()

        return intersectArea / (area1 + area2 - intersectArea)
    }
}
