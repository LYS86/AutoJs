package com.tflite.yolo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.support.common.FileUtil as TensorFlowFileUtil

object FileUtil {
    /**
     * 从外部存储加载模型文件。
     * @param filePath 模型文件路径。
     * @return MappedByteBuffer，表示模型文件的映射缓冲区。
     */
    @Throws(IOException::class)
    fun loadModel(filePath: String): MappedByteBuffer {
        if (filePath.isEmpty()) {
            throw IllegalArgumentException("File path is empty")
        }
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw IOException("File $filePath not exists or is not a file")
        }
        FileInputStream(file).use { inputStream ->
            val fileChannel = inputStream.channel
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
        }
    }


    /**
     * 从文件路径加载标签列表。
     * @param filePath 标签文件路径。
     * @return 标签列表。
     */
    @Throws(IOException::class)
    fun fromFile(filePath: String): List<String> {
        if (filePath.isEmpty()) return emptyList()
        return FileInputStream(filePath).use { inputStream ->
            TensorFlowFileUtil.loadLabels(inputStream)
        }
    }


    /**
     * 从JSON字符串中加载标签列表。
     * @param str JSON字符串，包含标签信息。
     * @return 标签列表，如果输入的JSON字符串为空或不包含"names"字段，则返回空列表。
     */
    fun fromJson(str: String): List<String> {
        if (str.isEmpty()) {
            return emptyList()
        }
        try {
            val map = Gson().fromJson(str, Map::class.java)
            val namesAny = map["names"]
            if (namesAny is Map<*, *>) {
                return namesAny.values.map { it as String }
            }
        } catch (e: Exception) {
            Log.e("FileUtil", "${e.message}")
        }
        return emptyList()
    }

    /**
     * 绘制检测结果。
     * @param bitmap 原始图片。
     * @param results 检测结果。
     * @return 绘制检测结果的图片。
     */
    fun drawBoxes(bitmap: Bitmap, results: Array<Result>): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.RED
        }
        results.forEach { result ->
            result.draw(canvas, paint, bitmap)
        }
        return output
    }
}
