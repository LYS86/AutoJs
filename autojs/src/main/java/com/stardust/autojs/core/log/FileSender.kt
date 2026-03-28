package com.stardust.autojs.core.log

import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 文件日志发送器 - 实现日志写入本地文件和滚动备份
 */
class FileSender private constructor() : LogSink() {

    data class Config(
        var filePath: String,
        val maxFileSize: Long = DEFAULT_MAX_FILE_SIZE,
        val minLevel: Int = Log.VERBOSE,
        val maxBackupSize: Int = DEFAULT_MAX_BACKUP_SIZE
    ) {
        companion object {
            fun fromJson(json: String): Config {
                return Gson().fromJson(json, Config::class.java)
            }
        }
    }

    private var config: Config? = null
    private val lock = Any()

    fun setConfig(config: Config) {
        this.config = config
    }

    fun setConfig(json: String) {
        this.config = Config.fromJson(json)
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        if (!super.isLoggable(tag, priority)) return false
        val config = this.config ?: return false
        return priority >= config.minLevel
    }

    override fun send(message: String) {
        val config = this.config ?: return

        synchronized(lock) {
            try {
                val logFile = File(config.filePath)
                logFile.parentFile?.mkdirs()

                if (logFile.length() >= config.maxFileSize) {
                    rollLogFiles(config)
                }

                logFile.appendText(message+"\n")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to write log to file", e)
            }
        }
    }


    private fun rollLogFiles(config: Config) {
        val baseFile = File(config.filePath)
        val parent = baseFile.parent ?: return
        val name = baseFile.nameWithoutExtension
        val ext = baseFile.extension

        val oldestBackup = File(parent, getBackupFileName(name, ext, config.maxBackupSize))
        if (oldestBackup.exists()) {
            oldestBackup.delete()
        }

        for (i in config.maxBackupSize - 1 downTo 1) {
            val oldFile = File(parent, getBackupFileName(name, ext, i))
            val newFile = File(parent, getBackupFileName(name, ext, i + 1))
            if (oldFile.exists()) {
                oldFile.renameTo(newFile)
            }
        }

        val firstBackup = File(parent, getBackupFileName(name, ext, 1))
        baseFile.renameTo(firstBackup)
    }

    private fun getBackupFileName(name: String, ext: String, index: Int): String {
        return if (ext.isEmpty()) {
            "${name}${index}.txt"
        } else {
            "${name}${index}.${ext}"
        }
    }

    companion object {
        private const val TAG = "FileSender"
        private const val DEFAULT_MAX_FILE_SIZE = 512 * 1024L // 512KB
        private const val DEFAULT_MAX_BACKUP_SIZE = 5

        @Volatile
        private var instance: FileSender? = null

        @JvmStatic
        fun getInstance(): FileSender {
            return instance ?: synchronized(this) {
                instance ?: FileSender().also { instance = it }
            }
        }
    }
}
