package org.autojs.autojs.ui.doc

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import timber.log.Timber
import java.io.IOException

/**
 * 本地文档 HTTP 服务器：将 assets/docs 映射到 http://127.0.0.1:<port>/，
 * 供外部浏览器（Custom Tabs）查看本地文档。
 */
object DocsServer {

    private const val ASSETS_ROOT = "docs"

    private var server: NanoHTTPD? = null
    private var applicationContext: Context? = null

    /**
     * 返回本地文档的绝对 URL（path 为相对路径，如 "globals.html#anchor"）。
     * 首次调用时启动服务器（懒启动，随进程存活，不主动停止）。
     */
    @JvmStatic
    @Synchronized
    fun url(context: Context, path: String): String {
        val appContext = applicationContext ?: context.applicationContext.also { applicationContext = it }
        val s = server ?: start(appContext).also { server = it }
        return "http://127.0.0.1:${s.listeningPort}/$path"
    }

    private fun start(context: Context): NanoHTTPD {
        val httpServer = object : NanoHTTPD("127.0.0.1", 0) {
            override fun serve(session: IHTTPSession): Response {
                var uri = session.uri
                if (uri == "/") {
                    uri = "/index.html"
                }
                return try {
                    val input = context.assets.open(ASSETS_ROOT + uri)
                    newChunkedResponse(Response.Status.OK, mimeOf(uri), input)
                } catch (e: IOException) {
                    Timber.w(e, "Local docs not found: %s", uri)
                    newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found: $uri")
                }
            }
        }
        httpServer.start()
        return httpServer
    }

    private fun mimeOf(uri: String): String = when (uri.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js" -> "application/javascript"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "svg" -> "image/svg+xml"
        "woff2" -> "font/woff2"
        "ico" -> "image/x-icon"
        else -> "application/octet-stream"
    }
}
