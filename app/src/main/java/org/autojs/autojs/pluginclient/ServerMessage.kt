package org.autojs.autojs.pluginclient

import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName
import org.autojs.autojs.BuildConfig

//服务器下发信息
data class ServerMessage(
    val type: String = "",
    val data: Data = Data(),
    @SerializedName("message_id")
    val messageId: Long = 0L
) {
    companion object {
        fun create(string: String): ServerMessage {
            return Gson().fromJson(string, ServerMessage::class.java)
        }
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }
}

//客户端发送信息
data class ClientMessage(
    val type: String = "",
    val data: Any,
    @SerializedName("message_id")
    val messageId: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    fun toJsonObject(): JsonObject {
        return Gson().toJsonTree(this).asJsonObject
    }
}

data class Data(
    val id: String = "",
    val name: String = "test.js",
    val script: String = "",
    val command: String = ""
)

data class Log(
    val message: String = "", val log: String = ""
)

data class DeviceInfo(
    val deviceName: String = "${Build.BRAND} ${Build.MODEL}",
    val deviceId: String = "unknown",
    val clientVersion: Int = 3,
    val appName: String = BuildConfig.APPLICATION_ID,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val appVersionCode: Int = BuildConfig.VERSION_CODE
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    fun toJsonObject(): JsonObject {
        return Gson().toJsonTree(this).asJsonObject
    }
}