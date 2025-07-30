package org.autojs.autojs.github.model

import com.google.gson.annotations.SerializedName

/**
 * @param name 用户名
 * @param avatarUrl 头像链接
 * @param htmlUrl 用户主页链接
 */
data class Contributor(
    @SerializedName("login")
    val name: String,
    @SerializedName("avatar_url")
    val avatarUrl: String,
    @SerializedName("html_url")
    val htmlUrl: String
)
