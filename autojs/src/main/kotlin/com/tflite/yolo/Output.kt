package com.tflite.yolo

import android.graphics.RectF
import android.util.Log

object Output {
    /**
     * 解析YOLO模型的输出
     */
    fun parseOutput(outputArray: FloatArray, data: ModelData): Array<Result> {
        return when (data.task) {
            "classify" -> arrayOf(classify(outputArray, data))
            "detect" -> when (data.model) {
                "yolo10" -> yolo10(outputArray, data).toTypedArray()
                else -> yolo(outputArray, data).toTypedArray()
            }

            else -> {
                Log.w("Output", "未知任务类型: ${data.task}")
                emptyArray()
            }
        }
    }

    fun setShape(shape: IntArray, data: ModelData) {
        if (data.task == "classify") return
        when {
            data.model == "yolo10" -> {
                require(shape[1] == 300 && shape[2] == 6) { "Invalid YOLOv10 output shape" }
                data.numDetections = shape[1]
            }

            else -> {
                data.numClasses = shape[1] - 4
                data.numDetections = shape[2]
            }
        }
    }

    private fun classify(output: FloatArray, data: ModelData): Result {
        val results = output.mapIndexed { index, prob ->
            Result(
                name = data.labels.getOrElse(index) { "unknown" },
                cnf = prob,
                id = index
            )
        }
        return results.maxByOrNull { it.cnf }?.apply {
            Result.ofClassify(
                name = this.name,
                cnf = this.cnf,
                id = this.id,
                allResults = results
            )
        } ?: Result(name = "", cnf = 0f)
    }

    private fun yolo10(outputArray: FloatArray, data: ModelData): List<Result> {
        val results = ArrayList<Result>()
        repeat(data.numDetections) { i ->
            val score = outputArray[i * 6 + 4]
            if (score >= data.args.conf) {
                results.add(
                    Result.ofLTRB(
                    outputArray[i * 6],
                    outputArray[i * 6 + 1],
                    outputArray[i * 6 + 2],
                    outputArray[i * 6 + 3],
                    score,
                    outputArray[i * 6 + 5].toInt(),
                    data.labels.getOrElse(outputArray[i * 6 + 5].toInt()) { "unknown" }
                ))
            }
        }
        return applyNMS(results, data)
    }

    private fun yolo(outputArray: FloatArray, data: ModelData): List<Result> {
        val results = ArrayList<Result>()
        val offsets = IntArray(data.numClasses + 4) { i ->
            when (i) {
                0 -> 0
                1 -> data.numDetections
                2 -> data.numDetections * 2
                3 -> data.numDetections * 3
                else -> data.numDetections * (4 + i - 4)
            }
        }

        repeat(data.numDetections) { i ->
            val baseOffset = i + data.numDetections * 4
            var maxScore = 0f
            var cls = 0

            repeat(data.numClasses) { j ->
                val score = outputArray[baseOffset + j * data.numDetections]
                if (score > maxScore) {
                    maxScore = score
                    cls = j
                }
            }

            if (maxScore >= data.args.conf) {
                results.add(
                    Result.ofXYWH(
                    outputArray[i + offsets[0]],
                    outputArray[i + offsets[1]],
                    outputArray[i + offsets[2]],
                    outputArray[i + offsets[3]],
                    maxScore,
                    cls,
                    data.labels.getOrElse(cls) { "unknown" }
                ))
            }
        }
        return applyNMS(results, data)
    }

    private fun applyNMS(results: List<Result>, data: ModelData): List<Result> {
        if (results.isEmpty() || data.args.nms) return results

        val resultsList = results.toMutableList().apply {
            sortWith { r1, r2 -> (r2.cnf - r1.cnf).toInt() }
        }
        val keep = BooleanArray(results.size) { true }

        for (i in results.indices) {
            if (!keep[i]) continue
            val r1 = results[i]
            for (j in i + 1 until results.size) {
                if (!keep[j]) continue
                val r2 = results[j]
                if (r1.id == r2.id && calculateIoU(r1.rect, r2.rect) > data.args.iou) {
                    keep[j] = false
                }
            }
        }
        return resultsList.filterIndexed { index, _ -> keep[index] }
    }

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