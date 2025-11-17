package com.tflite.yolo

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import timber.log.Timber

data class ModelData(
    var description: String = "",
    var author: String = "",
    var date: String = "",
    var version: String = "",
    var license: String = "",
    var docs: String = "",
    var model: String = "yolo8",
    var stride: Int = 32,
    var task: String = "detect",
    var batch: Int = 1,
    @SerializedName("imgsz") val imageSize: List<Int> = listOf(640, 640),
    var names: Map<Int, String> = emptyMap(),
    @SerializedName("num_detections") var numDetections: Int = 0,
    @SerializedName("num_classes") var numClasses: Int = 0,
    var args: Args = Args()
) {
    data class Args(
        var batch: Int = 1,
        var half: Boolean = false,
        var int8: Boolean = false,
        var nms: Boolean = false,
        var conf: Float = 0.25F,
        var iou: Float = 0.7F
    )

    companion object {
        fun ofJson(json: String): ModelData {
            if (json.isBlank()) return ModelData()
            return try {
                Gson().fromJson(json, ModelData::class.java) ?: ModelData()
            } catch (e: Exception) {
                Timber.w(e)
                ModelData()
            }
        }
    }

    var labels: List<String>
        get() = names.entries.sortedBy { it.key }.map { it.value }
        set(value) {
            names = value.mapIndexed { index, label -> index to label }.toMap()
        }

    fun toJson(): String = Gson().toJson(this)
    override fun toString(): String = toJson()
}