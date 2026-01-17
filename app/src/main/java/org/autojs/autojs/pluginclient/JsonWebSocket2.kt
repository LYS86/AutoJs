package org.autojs.autojs.pluginclient

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import timber.log.Timber
import java.io.Closeable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class JsonWebSocket2(client: OkHttpClient, request: Request) : WebSocketListener(), Closeable {

    data class Bytes(
        val md5: String,
        val byteString: ByteString,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val webSocket: WebSocket = client.newWebSocket(request, this)

    private val dataChannel = Channel<ServerMessage>(Channel.BUFFERED)
    private val bytesChannel = Channel<Bytes>(Channel.BUFFERED)
    private val closed = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    val dataFlow: Flow<ServerMessage> = dataChannel.receiveAsFlow()
    val bytesFlow: Flow<Bytes> = bytesChannel.receiveAsFlow()

    // 状态回调接口
    interface StateCallback {
        fun onConnected()
        fun onFailed(exception: Throwable)
        fun onClosed()
    }

    private var stateCallback: StateCallback? = null

    fun stateCallback(callback: StateCallback) {
        stateCallback = callback
    }

    // 在 JsonWebSocket2 类中添加
    constructor(url: String) : this(
        OkHttpClient.Builder()
            .pingInterval(1, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build(),
        Request.Builder().url(url).build()
    )

    override fun onMessage(webSocket: WebSocket, text: String) {
        Timber.d("onMessage: $text")
        scope.launch {
            try {
                val message = ServerMessage.create(text)
                dataChannel.send(message)
            } catch (e: Exception) {
                Timber.w(e, "parse json error: $text")
            }
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Timber.d("onMessage(bytes): $bytes")
        scope.launch {
            val md5 = bytes.md5().hex()
            bytesChannel.send(Bytes(md5, bytes))
        }
    }

    fun write(json: String): Boolean {
        Timber.d("send: $json")
        return webSocket.send(json)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Timber.d("onOpen: $response")
        stateCallback?.onConnected()
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Timber.d("onClosed: $code, reason=$reason")
        stateCallback?.onClosed()
        close()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Timber.e(t, "onFailure, response=$response")
        stateCallback?.onFailed(t)
        close()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            dataChannel.close()
            bytesChannel.close()
            webSocket.close(1000, "close")
        }
    }
}