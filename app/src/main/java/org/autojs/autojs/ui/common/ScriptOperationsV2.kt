package org.autojs.autojs.ui.common

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.stardust.pio.PFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.model.explorer.Explorer
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerFileItem
import org.autojs.autojs.model.explorer.ExplorerPage
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.util.FileInfo
import org.autojs.autojs.util.parseContentUri
import timber.log.Timber
import java.io.InputStream

class ScriptOperationsV2(
    private val context: Context,
    private val view: View? = null,
    private val currentDirectory: ScriptFile = ScriptFile(Pref.getScriptDirPath()),
    private val explorer: Explorer = Explorers.workspace(),
    private val explorerPage: ExplorerPage = ExplorerDirPage(currentDirectory, null)
) {

    /**
     * 处理uri,将文件复制到指定位置
     */
    suspend fun importFile(uri: Uri): Boolean {
        Timber.d("导入文件开始: uri=%s, 当前目录=%s", uri, currentDirectory.path)

        val inputStream = context.contentResolver.openInputStream(uri) ?: run {
            Timber.w("打开输入流失败: %s", uri)
            showMessage(R.string.text_import_fail)
            return false
        }

        val fileInfo = parseContentUri(context, uri)
        Timber.d("解析文件名=%s, 扩展名=%s", fileInfo.name, fileInfo.extension)

        return importFileInternal(inputStream, fileInfo)
    }

    /**
     * 处理文件路径导入
     */
    suspend fun importFile(filePath: String): Boolean {
        Timber.d("导入文件开始: filePath=%s, 当前目录=%s", filePath, currentDirectory.path)

        val sourceFile = ScriptFile(filePath)
        if (!sourceFile.exists()) {
            Timber.w("源文件不存在: %s", filePath)
            showMessage(R.string.text_import_fail)
            return false
        }

        val fileName = PFiles.getNameWithoutExtension(filePath)
        val fileExtension = PFiles.getExtension(filePath)
        val fileInfo = FileInfo(fileName, fileExtension)

        return importFileInternal(sourceFile, fileInfo)
    }

    /**
     * 处理sharedText导入
     */
    suspend fun importSharedText(sharedText: String, fileName: String = "shared_text", fileExtension: String = "js"): Boolean {
        Timber.d("导入sharedText开始: 文件名=%s, 扩展名=%s, 内容长度=%d", fileName, fileExtension, sharedText.length)

        return try {
            val pathTo = generateTargetPath(FileInfo(fileName, fileExtension))
            Timber.d("写入sharedText到: %s", pathTo)
            PFiles.write(pathTo, sharedText)
            withContext(Dispatchers.Main) {
                copyResult(true, pathTo)
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "导入sharedText异常")
            withContext(Dispatchers.Main) {
                showMessage(R.string.text_import_fail)
            }
            false
        }
    }

    private suspend fun importFileInternal(inputStream: InputStream, fileInfo: FileInfo): Boolean {
        return try {
            val pathTo = generateTargetPath(fileInfo)
            Timber.d("复制流到: %s", pathTo)
            val success = PFiles.copyStream(inputStream, pathTo)
            withContext(Dispatchers.Main) {
                copyResult(success, pathTo)
                success
            }
        } catch (e: Exception) {
            Timber.e(e, "导入文件异常")
            withContext(Dispatchers.Main) {
                showMessage(R.string.text_import_fail)
            }
            false
        } finally {
            inputStream.closeSafely()
        }
    }

    private suspend fun importFileInternal(sourceFile: ScriptFile, fileInfo: FileInfo): Boolean {
        return try {
            val pathTo = generateTargetPath(fileInfo)
            Timber.d("复制文件到: %s", pathTo)
            val success = PFiles.copy(sourceFile.path, pathTo)
            withContext(Dispatchers.Main) {
                copyResult(success, pathTo)
                success
            }
        } catch (e: Exception) {
            Timber.e(e, "导入文件异常")
            withContext(Dispatchers.Main) {
                showMessage(R.string.text_import_fail)
            }
            false
        }
    }

    private fun generateTargetPath(fileInfo: FileInfo): String {
        val basePath = "${currentDirectory.path}/${fileInfo.name}"
        return PFiles.generateNotExistingPath(basePath, ".${fileInfo.extension}")
    }

    private fun copyResult(success: Boolean, pathTo: String) {
        if (success) {
            Timber.d("文件导入成功: %s", pathTo)
            notifyFileCreated(ScriptFile(pathTo))
            showMessage(R.string.text_import_succeed)
        } else {
            Timber.w("文件导入失败: %s", pathTo)
            showMessage(R.string.text_import_fail)
        }
    }

    private fun notifyFileCreated(scriptFile: ScriptFile) {
        val item = if (scriptFile.isDirectory) {
            ExplorerDirPage(scriptFile, explorerPage)
        } else {
            ExplorerFileItem(scriptFile, explorerPage)
        }
        explorer.notifyItemCreated(item)
    }

    private fun showMessage(resId: Int) {
        view?.let {
            Snackbar.make(it, resId, Snackbar.LENGTH_SHORT).show()
        } ?: Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }

    private fun InputStream.closeSafely() {
        try {
            close()
        } catch (e: Exception) {
            Timber.w(e, "关闭输入流异常")
        }
    }
}

