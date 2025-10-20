package org.autojs.autojs.external.open

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import org.autojs.autojs.R
import org.autojs.autojs.ui.common.ScriptOperations
import timber.log.Timber
import java.io.FileNotFoundException

class ImportIntentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            setIntent(it)
            try {
                handleIntent(it)
            } catch (e: Exception) {
                Timber.e(e)
                Toast.makeText(this, R.string.edit_and_run_handle_intent_error, Toast.LENGTH_LONG)
                    .show()
                finish()
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun handleIntent(intent: Intent) {
        var uri: Uri? = intent.data
        if (uri == null && Intent.ACTION_SEND == intent.action) {
            uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        }

        if (uri == null) {
            finish()
            return
        }

        when (uri.scheme) {
            "content" -> {
                val (name, ext) = parseUri(uri)
                contentResolver.openInputStream(uri)?.let { inputStream ->
                    ScriptOperations(this, null).importFile(name, inputStream, ext)
                        .subscribe { finish() }
                } ?: throw FileNotFoundException("Cannot open input stream for URI: $uri")
            }

            "file" -> {
                val path = intent.data?.path
                if (path.isNullOrEmpty()) finish()
                ScriptOperations(this, null).importFile(path).subscribe { finish() }
            }

            else -> throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")
        }

    }

    /**
     * 从 content:// URI 中提取文件名（不含后缀）与后缀
     * - 仅当 URI 为 content:// 时才有效
     * - 解析失败时返回默认值：名字=""，后缀="js"
     */
    private fun parseUri(uri: Uri): Pair<String, String> {
        val displayName = contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && idx >= 0) c.getString(idx) else null
        } ?: uri.pathSegments.lastOrNull() ?: ""
        val name = displayName.substringBeforeLast(".")
        val ext = displayName.substringAfterLast(".").takeIf { it.isNotEmpty() } ?: "js"
        return name to ext
    }
}