package org.autojs.autojs.github.model

import com.google.gson.annotations.SerializedName

/**
 * 文件数据类
 * @param name 文件名
 * @param path 文件路径
 * @param sha 文件SHA值
 * @param size 文件大小
 * @param url 下载链接
 * @param type 文件类型
 * @param content 文件内容（Base64编码）
 * */
data class Contents(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long,
    @SerializedName("download_url") val url: String?,
    val type: String,
    val content: String
) {
    val isFile: Boolean get() = type == "file"
    val isDir: Boolean get() = type == "dir"
}
