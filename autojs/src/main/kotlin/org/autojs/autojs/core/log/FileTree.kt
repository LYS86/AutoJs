package org.autojs.autojs.core.log

import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Timber Tree 实现日志持久化到文件系统
 */
class FileTree(
    private val logDir: File,
    private val maxFileSize: Long = 10 * 1024 * 1024, // 10MB
    private val minPriority: Int = Log.VERBOSE,
    private val tagFilters: List<String>? = null
) : Timber.DebugTree() {

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val dateFileNameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val logExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        if (priority < minPriority) return false

        if (tagFilters != null && tag != null) {
            return tagFilters.any { tag.contains(it, ignoreCase = false) }
        }

        return true
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isLoggable(tag, priority)) return

        val logMessage = buildLogMessage(priority, tag, message, t)
        logExecutor.submit {
            writeLogToFile(logMessage)
        }
    }

    private fun buildLogMessage(priority: Int, tag: String?, message: String, t: Throwable?): String {
        val timestamp = dateFormat.format(Date())
        val priorityChar = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "?"
        }

        val sb = StringBuilder()
        sb.append("$timestamp ")
        sb.append("$priorityChar/")
        sb.append(tag ?: "null")
        sb.append(": $message")
        return sb.toString()
    }

    private fun writeLogToFile(message: String) {
        try {
            val logFile = getCurrentLogFile()
            val fileWriter = FileWriter(logFile, true)
            fileWriter.use {
                it.append(message).append("\n")
                it.flush()
            }

            // 检查文件大小,如果超过则轮转
            if (logFile.length() > maxFileSize) {
                rotateLogFile(logFile)
            }
        } catch (e: IOException) {
            // 防止递归记录错误
            Log.e("FileTreeLogger", "Failed to write log to file", e)
        }
    }

    private fun getCurrentLogFile(): File {
        val todayDir = File(logDir, dateFileNameFormat.format(Date()))
        if (!todayDir.exists()) {
            todayDir.mkdirs()
        }

        var logFile = File(todayDir, "autojs-${dateFileNameFormat.format(Date())}.log")
        var index = 1

        while (logFile.exists() && logFile.length() > maxFileSize) {
            logFile = File(todayDir, "autojs-${dateFileNameFormat.format(Date())}-$index.log")
            index++
        }

        return logFile
    }

    private fun rotateLogFile(logFile: File) {
        val todayDir = logFile.parentFile ?: return
        val baseName = logFile.nameWithoutExtension
        val extension = logFile.extension

        var index = 1
        while (File(todayDir, "$baseName-$index.$extension").exists()) {
            index++
        }

        val newFile = File(todayDir, "$baseName-$index.$extension")
        logFile.renameTo(newFile)
    }

    /**
     * 清理旧的日志文件
     */
    fun cleanupOldLogs(daysToKeep: Int = 30) {
        logExecutor.submit {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L)

            logDir.listFiles()?.forEach { monthDir ->
                if (monthDir.isDirectory) {
                    monthDir.listFiles()?.forEach { logFile ->
                        if (logFile.lastModified() < cutoffTime) {
                            logFile.delete()
                        }
                    }

                    if (monthDir.listFiles()?.isEmpty() == true) {
                        monthDir.delete()
                    }
                }
            }
        }
    }

    /**
     * 获取所有日志文件
     */
    fun getLogFiles(): List<File> {
        val logFiles = mutableListOf<File>()

        logDir.listFiles()?.forEach { monthDir ->
            if (monthDir.isDirectory) {
                monthDir.listFiles()?.let {
                    logFiles.addAll(it)
                }
            }
        }

        return logFiles.sortedByDescending { it.lastModified() }
    }

    /**
     * 关闭日志器
     */
    fun shutdown() {
        logExecutor.shutdown()
    }
}
