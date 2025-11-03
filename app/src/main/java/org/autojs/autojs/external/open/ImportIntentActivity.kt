package org.autojs.autojs.external.open

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import org.autojs.autojs.util.parseContentUri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import org.autojs.autojs.R
import org.autojs.autojs.ui.common.ScriptOperationsV2
import timber.log.Timber

class ImportIntentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntentSafely(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentSafely(intent)
    }

    private fun handleIntentSafely(intent: Intent) {
        try {
            handleIntent(intent)
        } catch (exception: Exception) {
            Timber.e(exception, "处理Intent时发生错误")
            showErrorMessage()
            finish()
        }
    }

    /*显示信息*/
    private fun showErrorMessage() {
        Toast.makeText(this, R.string.edit_and_run_handle_intent_error, Toast.LENGTH_LONG).show()
    }

    /*处理Intent*/
    private fun handleIntent(intent: Intent) {
        Timber.d("接收Intent: $intent")

        when (intent.action) {
            Intent.ACTION_VIEW, Intent.ACTION_EDIT -> handleViewOrEditIntent(intent)
            Intent.ACTION_SEND -> handleSendIntent(intent)
            Intent.ACTION_SEND_MULTIPLE -> handleSendMultipleIntent(intent)
            else -> handleUnsupportedIntent(intent)
        }
    }

    /*处理查看，编辑intent*/
    private fun handleViewOrEditIntent(intent: Intent) {
        val uri = intent.data
        if (uri == null) {
            Timber.w("ACTION_VIEW/EDIT Intent的data为null")
            finish()
            return
        }
        processSingleUri(uri)
    }

    /*处理发送intent*/
    private fun handleSendIntent(intent: Intent) {
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        Timber.d("uri: $uri")

        if (uri == null) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            Timber.w("sharedText: $sharedText")
            finish()
            return
        }
        processSingleUri(uri)
    }

    private fun handleSendMultipleIntent(intent: Intent) {
        val uris =
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        if (uris.isNullOrEmpty()) {
            Timber.w("ACTION_SEND_MULTIPLE Intent的EXTRA_STREAM列表为空")
            finish()
            return
        }

        Timber.d("处理多文件分享，文件数量: ${uris.size}")
        // TODO: 待ScriptOperations重构后支持多文件处理
        // 目前先处理第一个文件
        processSingleUri(uris.first())
    }

    private fun handleUnsupportedIntent(intent: Intent) {
        Timber.w("不支持的intent: $intent")
        finish()
    }

    private fun processSingleUri(uri: Uri) {
        when (uri.scheme) {
            "content" -> processContentUri(uri)
            "file" -> processFileUri(uri)
            else -> throw IllegalArgumentException("不支持的URI协议: ${uri.scheme}")
        }
    }

    private fun processContentUri(uri: Uri) {
        val (fileName, fileExtension) = parseContentUri(this, uri)
        Timber.d("解析Content URI: fileName=$fileName, fileExtension=$fileExtension")
        // TODO: ScriptOperations需要重构以适配新的接口
        // ScriptOperations(this, null).importFile(fileName, inputStream, fileExtension)
        ScriptOperationsV2(this).importFile(uri)
        finishAfterTransition()
    }

    private fun processFileUri(uri: Uri) {
        val filePath = uri.path
        if (filePath.isNullOrEmpty()) {
            Timber.w("File URI的路径为空")
            finish()
            return
        }

        // TODO: ScriptOperations需要重构以适配新的接口
        // ScriptOperations(this, null).importFile(filePath).subscribe { finish() }
        Timber.i("File URI处理完成: $filePath")
        finish()
    }

}