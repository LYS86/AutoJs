package com.tflite.yolo

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.mozilla.javascript.NativeObject
import timber.log.Timber


data class Config(
    @SerializedName("model") val modelPath: String = "",
    @SerializedName("labels") val labelsPath: String = "",
    @SerializedName("gpu") val useGpu: Boolean = true,
    ) {
    companion object {
        fun ofJson(json: String): Config {
            if (json.isBlank()) return Config()
            return try {
                Gson().fromJson(json, Config::class.java) ?: Config()
            } catch (e: Exception) {
                Timber.w(e)
                Config()
            }
        }

        fun ofNative(nativeObject: NativeObject): Config {
            return try {
                ofJson(Gson().toJson(nativeObject))
            } catch (e: Exception) {
                Timber.w(e)
                Config()
            }
        }
    }

    fun toJson(): String {
        return Gson().toJson(this)

    }

    override fun toString(): String = toJson()
}