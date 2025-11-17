package org.autojs.autojs.ui.doc

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import org.autojs.autojs.PrefV2
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ActivityDocumentationBinding
import org.autojs.autojs.ui.BaseActivityV2

/**
 * 文档浏览Activity
 * 显示应用帮助文档和教程
 */
class DocumentationActivity : BaseActivityV2() {

    companion object {
        const val EXTRA_URL = "url"
    }

    private lateinit var binding: ActivityDocumentationBinding
    private lateinit var webView: WebView
    private lateinit var backPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupWebView()
        setupBackPressedCallback()
    }


    private fun setupToolbar() {
        setToolbarAsBack(getString(R.string.text_tutorial))
    }


    private fun setupWebView() {
        webView = binding.ewebView.webView
        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            PrefV2.getDocumentationUrl() + "index.html"
        }
        webView.loadUrl(url)
    }

    private fun setupBackPressedCallback() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    webView.canGoBack() -> {
                        webView.goBack()
                    }
                    else -> {
                        finish()
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.isEnabled = false
    }
}