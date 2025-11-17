package org.autojs.autojs.external.open

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stardust.pio.PFiles
import org.autojs.autojs.R
import org.autojs.autojs.ui.edit.EditActivity
import timber.log.Timber
import java.io.File

/**
 * Created by Stardust on 2017/2/2.
 * 处理外部编辑意图的Activity
 */
class EditIntentActivity : AppCompatActivity() {

    companion object {
        private const val EXTERNAL_FILES = "external_files"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            handleIntent()
        } catch (e: Exception) {
            Timber.e(e, "处理编辑意图时发生错误")
            Toast.makeText(this, R.string.edit_and_run_handle_intent_error, Toast.LENGTH_LONG)
                .show()
        }
        finish()
    }

    private fun handleIntent() {
        val uri = intent.data ?: return

        val filePath = when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> {
                uri.path
            }

            else -> {
                extractExternalFilePath(uri)
            }
        }

        if (filePath.isNullOrEmpty().not()) {
            EditActivity.editFile(this, filePath, false)
        } else {
            EditActivity.editFile(this, uri, false)
        }
    }

    private fun extractExternalFilePath(uri: Uri): String? {
        val uriPath = uri.path ?: return null
        val index = uriPath.indexOf(EXTERNAL_FILES)

        if (index < 0) return null

        val relativePath = uriPath.substring(index + EXTERNAL_FILES.length)

        // 首先尝试直接路径
        if (PFiles.exists(relativePath)) {
            return relativePath
        }

        // 然后尝试外部存储目录下的路径
        val fullPath = File(Environment.getExternalStorageDirectory(), relativePath).path
        return if (PFiles.exists(fullPath)) fullPath else null
    }
}