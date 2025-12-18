package org.autojs.autojs.ui.doc

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.databinding.FragmentOnlineDocsBinding
import org.autojs.autojs.ui.main.QueryEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class DocsFragment : Fragment(R.layout.fragment_online_docs) {

    private var _binding: FragmentOnlineDocsBinding? = null
    private val binding get() = _binding!!
    private lateinit var webView: WebView
    private var indexUrl: String = ""
    private var previousQuery: String? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnlineDocsBinding.bind(view)
        webView = binding.ewebView.webView
        val swipeRefresh: SwipeRefreshLayout = binding.ewebView.swipeRefreshLayout
        swipeRefresh.setOnRefreshListener { handleRefresh() }
        restoreWebViewState(savedInstanceState)
        webView.setOnScrollChangeListener { _, _, _, _, _ ->
            updateBackPressCallback()
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                updateBackPressCallback()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateBackPressCallback()
    }

    private fun updateBackPressCallback() {
        onBackPressedCallback.isEnabled = isAdded && isVisible && webView.canGoBack()
    }

    private fun handleRefresh() {
        if (webView.url == indexUrl) {
            loadUrl()
        } else {
            binding.ewebView.onRefresh()
        }
        binding.ewebView.swipeRefreshLayout.isRefreshing = false
    }

    private fun restoreWebViewState(state: Bundle?) {
        val savedState = arguments?.getBundle(KEY_SAVED_WEBVIEW_STATE)
        if (savedState != null) {
            webView.restoreState(savedState)
            updateBackPressCallback()
        } else {
            loadUrl()
        }
    }

    private fun loadUrl() {
        indexUrl = "${Pref.getDocumentationUrl()}index.html"
        webView.loadUrl(indexUrl)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val webViewState = Bundle().apply {
            webView.saveState(this)
        }
        arguments?.putBundle(KEY_SAVED_WEBVIEW_STATE, webViewState)
    }

    @Subscribe
    fun onQuerySummit(event: QueryEvent) {
        if (!isAdded || !isVisible) return

        when (event) {
            QueryEvent.CLEAR -> {
                webView.clearMatches()
                previousQuery = null
            }

            else -> handleSearchQuery(event)
        }
    }

    private fun handleSearchQuery(event: QueryEvent) {
        when {
            event.isFindForward -> {
                webView.findNext(false)
            }

            event.query == previousQuery -> {
                webView.findNext(true)
            }

            else -> {
                webView.findAllAsync(event.query)
                previousQuery = event.query
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    companion object {
        private const val KEY_SAVED_WEBVIEW_STATE = "savedWebViewState"
    }
}