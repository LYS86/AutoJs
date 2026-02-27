package org.autojs.autojs.ui.doc

import android.os.Bundle
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.widget.EWebView

class DocumentationActivity : BaseActivity() {

    private lateinit var eWebView: EWebView
    private lateinit var webView: android.webkit.WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentation)
        eWebView = findViewById(R.id.eweb_view)
        setUpViews()
    }

    private fun setUpViews() {
        setToolbarAsBack(getString(R.string.text_tutorial))
        webView = eWebView.webView
        val url = intent.getStringExtra(EXTRA_URL) ?: "${Pref.getDocumentationUrl()}index.html"
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val EXTRA_URL = "url"
    }
}
