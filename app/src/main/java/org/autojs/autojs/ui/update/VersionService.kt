package org.autojs.autojs.ui.update

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.google.gson.Gson
import org.autojs.autojs.github.AppGithub
import org.autojs.autojs.github.model.ApiResult
import org.autojs.autojs.github.model.RateLimit
import org.autojs.autojs.github.model.Release

object VersionService {

    private fun getLocalFile(context: Context): String {
        return context.assets.open("latest.json").bufferedReader().use { it.readText() }
    }

    //    本地数据测试
    private fun testLocal(context: Context): ApiResult<Release> {
        val result = Gson().fromJson(getLocalFile(context), Release::class.java)
        return ApiResult.Success(result, RateLimit(10, 10))
    }


    /**
     * 测试版本检查功能
     */
    suspend fun test(context: Context) {
        checkForUpdate(context)
    }

    suspend fun checkForUpdate(context: Context) {
        val result = AppGithub.getLatest()
        val dialog = UpdateDialog(context)
        when (result) {
            is ApiResult.Success -> {
                when {
                    hasUpdate(context, result.data) -> dialog.needUpdateDialog(result.data)
                    else -> dialog.noUpdateDialog()
                }
            }

            is ApiResult.Error -> dialog.errorDialog(result.error)
        }
        dialog.show()

    }

    /**
     * 验证版本号，是否有更新
     * @param context 上下文
     * @param release github release
     */
    private fun hasUpdate(context: Context, release: Release): Boolean {
        return parseVersionCode(release) > getVersionCode(context)
    }


    /**
     * 获取本地版本号
     */
    private fun getVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(packageInfo)
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * 解析云端版本号，格式：v1.2.3-456789 -> 456789
     */
    private fun parseVersionCode(release: Release): Long {
        val parts = release.name.split('-')
        return parts.lastOrNull()?.toLongOrNull() ?: 0L
    }

}