// DevPluginService2.kt
package org.autojs.autojs.pluginclient

import android.os.Build
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.stardust.app.GlobalAppContext
import com.stardust.util.MapBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.autojs.autojs.BuildConfig
import org.autojs.autojs.PrefV2
import org.autojs.autojs.tool.NetworkTool
import timber.log.Timber
import java.io.File
import java.net.SocketTimeoutException

class DevPluginService2 private constructor() {

    sealed class State {
        object Connecting : State()
        object Connected : State()
        object Disconnected : State()
        data class Error(val exception: Throwable) : State()
    }

    /*本地保存的服务器地址*/
    var serverAddress by PrefV2.string("key_dev_plugin_server_address", NetworkTool.getLocalIp())
    private var lastConnected by PrefV2.boolean("websocket_last_connected", false)

    // 统一的状态 Flow
    private val _connectionState = MutableStateFlow<State>(State.Disconnected)

    // 公开的状态 Flow
    val connectionState: StateFlow<State> = _connectionState.asStateFlow()

    // 计算属性
    val isConnected: Boolean get() = connectionState.value is State.Connected

    // 内部状态
    private val mBytes = HashMap<String, JsonWebSocket2.Bytes>()
    private val mRequiredBytesCommands = HashMap<String, JsonObject>()
    private var mSocket: JsonWebSocket2? = null
    private var socketJob: Job? = null
    private val mResponseHandler: DevPluginResponseHandler2
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val CLIENT_VERSION = 2
        private const val TYPE_HELLO = "hello"
        private const val TYPE_BYTES_COMMAND = "bytes_command"
        private const val HANDSHAKE_TIMEOUT = 10 * 1000L
        private const val PORT = 9317

        @Volatile
        private var instance: DevPluginService2? = null

        fun getInstance(): DevPluginService2 {
            return instance ?: synchronized(this) {
                instance ?: DevPluginService2().also {
                    instance = it
                }
            }
        }
    }

    init {
        val cache = File(GlobalAppContext.get().cacheDir, "remote_project")
        mResponseHandler = DevPluginResponseHandler2(cache)

        coroutineScope.launch {
            connectionState.collect { state ->
                val newConnected = state is State.Connected
                if (lastConnected != newConnected) {
                    lastConnected = newConnected
                    Timber.d("Pref状态变化: $newConnected")
                }
            }
        }
    }

    fun restore() {
        when {
            !lastConnected -> return
            serverAddress.isEmpty() -> return
            isConnected -> return
            else -> connectToServer(serverAddress)
        }
    }

    /*断开连接*/
    fun disconnect() {
        socketJob?.cancel()
        socketJob = null
        mSocket?.close()
        mSocket = null
        _connectionState.value = State.Disconnected
    }

    /*连接服务器*/
    fun connectToServer(host: String) {
        serverAddress = host
        _connectionState.value = State.Connecting
        socketJob?.cancel()
        socketJob = null
        socketJob = coroutineScope.launch {
            try {
                socket(host).collect { socket ->
                    mSocket = socket
                    subscribeMessage(socket)
                    sayHelloToServer(socket)
                }
            } catch (error: Throwable) {
                _connectionState.value = State.Error(error)
            }
        }
    }

    private fun socket(host: String): Flow<JsonWebSocket2> {
        return flow {
            val (ip, port) = parseHost(host)
            Timber.d("host to $ip:$port")
            val url = buildUrl(ip, port)
            Timber.d("WebSocket URL: $url")
            val socket = JsonWebSocket2(url)
            emit(socket)
        }.flowOn(Dispatchers.IO)
    }

    private fun subscribeMessage(socket: JsonWebSocket2) {
        socket.stateCallback(object : JsonWebSocket2.StateCallback {
            override fun onConnected() {
                Timber.d("WebSocket 成功连接")
            }

            override fun onFailed(exception: Throwable) {
                Timber.d(exception, "WebSocket 连接出错")
                _connectionState.value = State.Error(exception)
                mSocket = null
            }

            override fun onClosed() {
                Timber.d("WebSocket 连接关闭")
                if (mSocket === socket) {
                    _connectionState.value = State.Disconnected
                    mSocket = null
                }
            }
        })

        coroutineScope.launch {
            try {
                launch {
                    socket.dataFlow.collect { data ->
                        onSocketData(socket, data)
                    }
                }

                launch {
                    socket.bytesFlow.collect { bytes ->
                        onSocketData(bytes)
                    }
                }
            } catch (e: Exception) {
                _connectionState.value = State.Error(e)
            }
        }
    }

    private fun onSocketData(jsonWebSocket: JsonWebSocket2, element: JsonElement) {
        if (!element.isJsonObject) {
            return
        }
        try {
            val obj = element.asJsonObject
            val typeElement = obj.get("type")
            if (typeElement == null || !typeElement.isJsonPrimitive) return
            when (typeElement.asString) {
                TYPE_HELLO -> {
                    onServerHello(jsonWebSocket, obj)
                }

                TYPE_BYTES_COMMAND -> {
                    handleBytesCommand(obj)
                }

                else -> {
                    coroutineScope.launch { mResponseHandler.handle(obj) }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "信息解析出错: $element")
        }
    }

    private fun handleBytesCommand(obj: JsonObject) {
        val md5 = obj.get("md5").asString
        val bytes = mBytes.remove(md5)
        if (bytes != null) {
            coroutineScope.launch { handleBytes(obj, bytes) }
        } else {
            mRequiredBytesCommands[md5] = obj
        }
    }

    private suspend fun handleBytes(obj: JsonObject, bytes: JsonWebSocket2.Bytes) {
        val dir = mResponseHandler.handleBytes(obj, bytes)
        obj.get("data").asJsonObject.add("dir", JsonPrimitive(dir.path))
        mResponseHandler.handle(obj)
    }

    private fun onSocketData(bytes: JsonWebSocket2.Bytes) {
        val command = mRequiredBytesCommands.remove(bytes.md5)
        if (command != null) {
            coroutineScope.launch { handleBytes(command, bytes) }
        } else {
            mBytes[bytes.md5] = bytes
        }
    }

    private class HandshakeSuccessException : Exception()

    private suspend fun sayHelloToServer(socket: JsonWebSocket2) {
        writeMap(
            socket,
            TYPE_HELLO,
            MapBuilder<String, Any>().put("device_name", "${Build.BRAND} ${Build.MODEL}")
                .put("client_version", CLIENT_VERSION).put("app_version", BuildConfig.VERSION_NAME)
                .put("app_version_code", BuildConfig.VERSION_CODE).build()
        )
        try {
            withTimeout(HANDSHAKE_TIMEOUT) {
                connectionState.collect { state ->
                    if (state is State.Connected && mSocket === socket) {
                        throw HandshakeSuccessException()
                    }
                }
            }
        } catch (_: HandshakeSuccessException) {
        } catch (_: Exception) {
            onHandshakeTimeout(socket)
        }
    }

    private fun onHandshakeTimeout(socket: JsonWebSocket2) {
        val exception = SocketTimeoutException("Handshake timeout")
        _connectionState.value = State.Error(exception)
        socket.close()
    }

    private fun onServerHello(jsonWebSocket: JsonWebSocket2, message: JsonObject) {
        mSocket = jsonWebSocket
        _connectionState.value = State.Connected
    }

    fun log(log: String) {
        if (!isConnected) return
        val data = JsonObject().apply {
            addProperty("log", log)
        }
        write(mSocket!!, "log", data)
    }

    private fun write(socket: JsonWebSocket2, type: String, data: JsonObject): Boolean {
        val json = JsonObject().apply {
            addProperty("type", type)
            add("data", data)
        }
        return socket.write(json)
    }

    private fun writeMap(socket: JsonWebSocket2, type: String, map: Map<String, *>): Boolean {
        val data = JsonObject().apply {
            map.forEach { (key, value) ->
                when (value) {
                    is String -> addProperty(key, value)
                    is Char -> addProperty(key, value)
                    is Number -> addProperty(key, value)
                    is Boolean -> addProperty(key, value)
                    is JsonElement -> add(key, value)
                    else -> throw IllegalArgumentException("Unsupported type: ${value?.javaClass}")
                }
            }
        }
        return write(socket, type, data)
    }

    private fun parseHost(host: String): Pair<String, Int> {
        // 标准格式: host:port
        val lastColon = host.lastIndexOf(':')
        if (lastColon > 0 && lastColon < host.length - 1) {
            val portStr = host.substring(lastColon + 1)
            val port = portStr.toIntOrNull() ?: PORT
            return Pair(host.substring(0, lastColon), port)
        }

        return Pair(host, PORT)
    }

    private fun buildUrl(ip: String, port: Int): String {
        return when {
            ip.startsWith("ws://") || ip.startsWith("wss://") -> "$ip:$port"
            else -> "ws://$ip:$port"
        }
    }
}