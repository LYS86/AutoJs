package org.autojs.autojs.external.open

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
            if (sharedText.isNullOrEmpty()) {
                Timber.w("sharedText为空")
                finish()
                return
            }
            Timber.d("处理sharedText: 长度=${sharedText.length}")
            processSharedText(sharedText)
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
        lifecycleScope.launch {
            try {
                val success = ScriptOperationsV2(this@ImportIntentActivity).importFile(uri)
                if (success) {
                    Timber.d("Content URI导入成功")
                } else {
                    Timber.w("Content URI导入失败")
                }
            } catch (e: Exception) {
                Timber.e(e, "处理Content URI时发生错误")
                showErrorMessage()
            } finally {
                finish()
            }
        }
    }

    private fun processFileUri(uri: Uri) {
        val filePath = uri.path
        if (filePath.isNullOrEmpty()) {
            Timber.w("File URI的路径为空")
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                Timber.d("开始处理File URI: $filePath")
                val success = ScriptOperationsV2(this@ImportIntentActivity).importFile(filePath)
                if (success) {
                    Timber.d("File URI导入成功: $filePath")
                } else {
                    Timber.w("File URI导入失败: $filePath")
                }
            } catch (e: Exception) {
                Timber.e(e, "处理File URI时发生错误")
                showErrorMessage()
            } finally {
                finish()
            }
        }
    }

    private fun processSharedText(sharedText: String) {
        lifecycleScope.launch {
            try {
                Timber.d("开始处理sharedText: 长度=${sharedText.length}")

                val success = ScriptOperationsV2(this@ImportIntentActivity).importSharedText(
                    sharedText = sharedText
                )

                if (success) {
                    Timber.d("sharedText导入成功")
                } else {
                    Timber.w("sharedText导入失败")
                }
            } catch (e: Exception) {
                Timber.e(e, "处理sharedText时发生错误")
                showErrorMessage()
            } finally {
                finish()
            }
        }
    }

}