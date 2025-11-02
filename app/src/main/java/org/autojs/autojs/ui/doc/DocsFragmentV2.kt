package org.autojs.autojs.ui.doc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.autojs.autojs.Pref
import org.autojs.autojs.databinding.FragmentOnlineDocsBinding
import org.autojs.autojs.ui.main.QueryEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber

class DocsFragmentV2 : Fragment() {

    private lateinit var binding: FragmentOnlineDocsBinding
    private lateinit var webView: WebView
    private var indexUrl: String? = null
    private var previousQuery: String? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            Timber.d("WebView返回键被按下")
            if (webView.canGoBack()) {
                webView.goBack()
                Timber.d("WebView返回上一页")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        Timber.d("文档片段已创建")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnlineDocsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = binding.ewebView.webView
        val swipeRefresh: SwipeRefreshLayout = binding.ewebView.swipeRefreshLayout
        swipeRefresh.setOnRefreshListener { handleRefresh() }

        restoreWebViewState(savedInstanceState)

        // 根据WebView状态更新返回键回调
        webView.setOnScrollChangeListener { _, _, _, _, _ ->
            updateBackPressCallback()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

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
            Timber.d("WebView状态已恢复")
        } else {
            loadUrl()
        }
    }

    private fun loadUrl() {
        val defaultUrl = "${Pref.getDocumentationUrl()}index.html"
        indexUrl = arguments?.getString(ARGUMENT_URL) ?: defaultUrl

        webView.loadUrl(indexUrl!!)
        Timber.d("正在加载URL: $indexUrl")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val webViewState = Bundle().apply {
            webView.saveState(this)
        }
        arguments?.putBundle(KEY_SAVED_WEBVIEW_STATE, webViewState)
        Timber.d("WebView状态已保存")
    }

    @Subscribe
    fun onQuerySummit(event: QueryEvent) {
        if (!isAdded || !isVisible) return

        when (event) {
            QueryEvent.CLEAR -> {
                webView.clearMatches()
                previousQuery = null
                Timber.d("搜索已清除")
            }
            else -> handleSearchQuery(event)
        }
    }

    private fun handleSearchQuery(event: QueryEvent) {
        when {
            event.isFindForward -> {
                webView.findNext(false)
                Timber.d("向前查找下一个")
            }
            event.query == previousQuery -> {
                webView.findNext(true)
                Timber.d("查找下一个匹配项")
            }
            else -> {
                webView.findAllAsync(event.query)
                previousQuery = event.query
                Timber.d("新的搜索查询: ${event.query}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("文档片段视图已销毁")
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        Timber.d("文档片段已销毁")
    }

    companion object {
        const val ARGUMENT_URL = "url"
        private const val KEY_SAVED_WEBVIEW_STATE = "savedWebViewState"

        fun newInstance(url: String): DocsFragmentV2 {
            return DocsFragmentV2().apply {
                arguments = Bundle().apply {
                    putString(ARGUMENT_URL, url)
                }
            }
        }
    }
}