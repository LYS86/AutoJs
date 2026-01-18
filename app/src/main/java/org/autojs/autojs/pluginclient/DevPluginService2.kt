package org.autojs.autojs.pluginclient

import com.stardust.app.GlobalAppContext
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
import org.autojs.autojs.PrefV2
import org.autojs.autojs.core.log.WebSocketSender
import org.autojs.autojs.tool.NetworkTool
import timber.log.Timber
import java.io.File
import java.net.SocketTimeoutException

class DevPluginService2 private constructor() : WebSocketSender {

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
    private val mRequiredBytesCommands = HashMap<String, ServerMessage>()
    private var mSocket: JsonWebSocket2? = null
    private var socketJob: Job? = null
    private val mResponseHandler: DevPluginResponseHandler2
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    companion object {
        private const val TYPE_HELLO = "hello"
        private const val TYPE_BYTES_COMMAND = "bytes_command"
        private const val HANDSHAKE_TIMEOUT = 10 * 1000L
        private const val PORT = 9317
        val instance: DevPluginService2 by lazy { DevPluginService2() }
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
            val url = buildUrl(host)
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

    private fun onSocketData(jsonWebSocket: JsonWebSocket2, message: ServerMessage) {
        try {
            when (message.type) {
                TYPE_HELLO -> {
                    onServerHello(jsonWebSocket)
                }

                TYPE_BYTES_COMMAND -> {
                    handleBytesCommand(message)
                }

                else -> {
                    coroutineScope.launch { mResponseHandler.handle(message) }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "信息解析出错: $message")
        }
    }

    private fun handleBytesCommand(message: ServerMessage) {
        val md5 = message.md5
        val bytes = mBytes.remove(md5)
        if (bytes != null) {
            coroutineScope.launch { handleBytes(message, bytes) }
        } else {
            mRequiredBytesCommands[md5] = message
        }
    }

    private suspend fun handleBytes(message: ServerMessage, bytes: JsonWebSocket2.Bytes) {
        val dir = mResponseHandler.handleBytes(message.data, bytes)
        mResponseHandler.handle(message, dir)
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

        val message = ClientMessage(
            type = "hello",
            data = DeviceInfo()
        ).toJson()
        socket.write(message)

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

    private fun onServerHello(jsonWebSocket: JsonWebSocket2) {
        mSocket = jsonWebSocket
        _connectionState.value = State.Connected
    }

    override fun sendLog(message: String) {
        coroutineScope.launch {
            val clientMessage = ClientMessage(
                type = "log",
                data = Log(message, message)
            )
            mSocket?.write(clientMessage.toJson())
        }
    }

    private fun parseHost(host: String): Pair<String, Int> {
        val lastColon = host.lastIndexOf(':')
        if (lastColon > 0 && lastColon < host.length - 1) {
            val portStr = host.substring(lastColon + 1)
            val port = portStr.toIntOrNull() ?: PORT
            return Pair(host.take(lastColon), port)
        }

        return Pair(host, PORT)
    }

    private fun buildUrl(host: String): String {
        val (ip, port) = parseHost(host)
        return when {
            ip.startsWith("ws://") || ip.startsWith("wss://") -> "$ip:$port"
            else -> "ws://$ip:$port"
        }
    }
}