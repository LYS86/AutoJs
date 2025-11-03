package org.autojs.autojs.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import timber.log.Timber

/**
 * 从 content:// URI 解析文件名和扩展名
 * @return [FileInfo]
 */
fun parseContentUri(context: Context, uri: Uri): FileInfo {
    Timber.d("解析Content URI: uri=%s", uri)
    val displayName = try {
        context.contentResolver.query(
            uri, 
            arrayOf(OpenableColumns.DISPLAY_NAME), 
            null, null, null
        )?.use { cursor ->
            val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && displayNameIndex >= 0) {
                cursor.getString(displayNameIndex)
            } else {
                uri.pathSegments.lastOrNull()
            }
        } ?: uri.pathSegments.lastOrNull() ?: ""
    } catch (e: SecurityException) {
        Timber.e(e, "查询Content URI时发生安全异常: %s", uri)
        uri.pathSegments.lastOrNull() ?: ""
    } catch (e: Exception) {
        Timber.e(e, "查询Content URI时发生未知错误: %s", uri)
        uri.pathSegments.lastOrNull() ?: ""
    }

    val fileName = displayName.substringBeforeLast('.')
    val fileExtension = displayName.substringAfterLast('.').takeIf { it.isNotEmpty() } ?: DEFAULT_FILE_EXTENSION

    Timber.d("解析Content URI结果: 文件名=%s, 文件扩展名=%s", fileName, fileExtension)
    return FileInfo(fileName, fileExtension)
}

/**
 * 文件信息。
 * @param Name 文件名（不含扩展名）。
 * @param Extension 文件扩展名。
 */
data class FileInfo(
    val name: String,
    val extension: String
)
private const val DEFAULT_FILE_EXTENSION = "js"
