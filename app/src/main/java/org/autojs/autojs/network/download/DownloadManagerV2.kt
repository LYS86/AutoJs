package org.autojs.autojs.network.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import java.io.File

class DownloadManagerV2(private val context: Context) {

    private val systemDownloadManager: DownloadManager?
        get() = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    /**
     * 使用系统下载管理器下载文件
     *
     * @param url 文件下载URL
     * @param fileName 保存的文件名
     * @param subDirectory 子目录（可选，默认在Download/AutoJs）
     * @param title 通知标题（可选）
     * @param description 通知描述
     * @param showNotification 是否显示通知
     * @return 下载任务ID，失败返回-1
     */
    fun downloadFile(
        url: String,
        fileName: String,
        subDirectory: String = "AutoJs",
        title: String? = null,
        description: String = url,
        showNotification: Boolean = true
    ): Long {
        val downloadManager = systemDownloadManager ?: return -1

        // 创建目标目录
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            subDirectory
        ).apply { mkdirs() }

        val outputFile = File(downloadDir, fileName)

        try {
            val request = DownloadManager.Request(url.toUri()).apply {
                if (showNotification) {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setTitle(title ?: fileName)
                    setDescription(description)
                } else {
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                }
                setDestinationUri(Uri.fromFile(outputFile))
                setMimeType(getMimeType(fileName))
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)

                addRequestHeader("User-Agent", "Mozilla/5.0")
                addRequestHeader("Accept", "application/octet-stream")
            }

            return downloadManager.enqueue(request)
        } catch (_: Exception) {
            return -1
        }
    }

    /**
     * 获取文件的MIME类型
     */
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".apk") -> "application/vnd.android.package-archive"
            fileName.endsWith(".zip") -> "application/zip"
            fileName.endsWith(".js") -> "text/javascript"
            fileName.endsWith(".json") -> "application/json"
            else -> "application/octet-stream"
        }
    }

    /**
     * 移除下载任务
     *
     * @param downloadId 下载任务ID
     */
    fun removeDownload(downloadId: Long) {
        systemDownloadManager?.remove(downloadId)
    }
}