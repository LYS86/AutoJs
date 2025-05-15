package com.tflite.yolo

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

private const val TAG = "Config"

data class ModelData(
    @SerializedName("description") var description: String = "",
    @SerializedName("author") var author: String = "",
    @SerializedName("date") var date: String = "",
    @SerializedName("version") var version: String = "",
    @SerializedName("license") var license: String = "",
    @SerializedName("docs") var docs: String = "",
    @SerializedName("model") val model: String = "yolo8",
    @SerializedName("stride") var stride: Int = 32,
    @SerializedName("task") var task: String = "detect",
    @SerializedName("batch") val batch: Int = 1,
    @SerializedName("imgsz") val imageSize: List<Int> = listOf(640, 640),
    @SerializedName("names") var names: Map<Int, String> = emptyMap(),
    @SerializedName("num_detections") var numDetections: Int = 0,
    @SerializedName("num_classes") var numClasses: Int = 0,
    @SerializedName("args") var args: Args = Args()
) {
    data class Args(
        @SerializedName("batch") var batch: Int = 1,
        @SerializedName("half") var half: Boolean = false,
        @SerializedName("int8") var int8: Boolean = false,
        @SerializedName("nms") var nms: Boolean = false,
        @SerializedName("conf") var conf: Float = 0.25F,
        @SerializedName("iou") var iou: Float = 0.7F
    )

    companion object {
        fun ofJson(json: String): ModelData {
            if (json.isBlank()) return ModelData()
            return try {
                Gson().fromJson(json, ModelData::class.java) ?: ModelData()
            } catch (e: Exception) {
                Log.e(TAG, "error: ${e.message}")
                ModelData()
            }
        }
    }

    var labels: List<String>
        get() = names.entries.sortedBy { it.key }.map { it.value }
        set(value) {
            names = value.mapIndexed { index, label -> index to label }.toMap()
        }

    fun toJson(): String {
        return Gson().toJson(this)
    }

    override fun toString(): String = toJson()
}