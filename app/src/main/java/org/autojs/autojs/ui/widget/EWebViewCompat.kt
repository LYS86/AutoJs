package org.autojs.autojs.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.autojs.autojs.R
import org.autojs.autojs.theme.ThemeUtils
import java.util.concurrent.TimeUnit

class EWebViewCompat(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs), SwipeRefreshLayout.OnRefreshListener {

    val webView: WebView
    val swipeRefreshLayout: SwipeRefreshLayout
    private val progressBar: ProgressBar
    private val disposables = CompositeDisposable()

    init {
        inflate(context, R.layout.ewebview, this)
        webView = ViewCompat.requireViewById(this, R.id.web_view)
        swipeRefreshLayout = ViewCompat.requireViewById(this, R.id.swipe_refresh_layout)
        progressBar = ViewCompat.requireViewById(this, R.id.progress_bar)
        swipeRefreshLayout.setOnRefreshListener(this)
        setUpWebView()
    }

    private fun setUpWebView() {
        val settings: WebSettings = webView.settings
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.loadWithOverviewMode = true
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.domStorageEnabled = true
        settings.displayZoomControls = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.isVisible = false
                swipeRefreshLayout.isRefreshing = false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                progressBar.isVisible = newProgress in 1..99
            }
        }

        applyDarkMode()
    }


    @Suppress("DEPRECATION")
    private fun applyDarkMode() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) return

        val isDark = ThemeUtils.isDarkMode(context)
        val settings = webView.settings
        val mode = if (isDark) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF

        WebSettingsCompat.setForceDark(
            settings,
            mode
        )

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(
                settings,
                WebSettingsCompat.DARK_STRATEGY_USER_AGENT_DARKENING_ONLY
            )
        }

        // TODO: When targetSdk >= 33, replace with:
        // if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        //     WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
        // }
    }

    override fun onRefresh() {
        webView.reload()
        disposables.add(
            Observable.timer(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { swipeRefreshLayout.isRefreshing = false }
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposables.clear()
    }
}
