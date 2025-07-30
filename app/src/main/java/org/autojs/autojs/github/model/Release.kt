package org.autojs.autojs.github.model

import com.google.gson.annotations.SerializedName

/**
 * GitHub Release 发布信息
 * @property tagName 版本标签
 * @property name 发布名称
 * @property assets 附件列表
 * @property body 发布内容
 * @property htmlUrl 发布页面链接
 * */

data class Release(
    @SerializedName("tag_name")
    val tagName: String,
    val name: String,
    val body: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    val assets: List<Asset>,
)


/**
 * GitHub Release 附件信息
 * @property name 附件文件名
 * @property url 下载链接
 * @property size 文件大小（字节）
 * @property contentType MIME类型
 */

data class Asset(
    val name: String,
    @SerializedName("browser_download_url")
    val url: String,
    val size: Long,
    @SerializedName("content_type")
    val contentType: String
) {
    val isApk by lazy {
        contentType.equals("application/vnd.android.package-archive", ignoreCase = true)
    }
}