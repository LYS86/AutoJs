package org.autojs.autojs.ui.doc

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import org.autojs.autojs.Pref
import org.autojs.autojs.databinding.FragmentOnlineDocsBinding
import org.autojs.autojs.ui.base.BaseFragment
import org.autojs.autojs.ui.main.QueryEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class DocsFragmentV2 : BaseFragment() {

    companion object {
        const val ARGUMENT_URL = "url"

        fun newInstance(url: String): DocsFragmentV2 {
            return DocsFragmentV2().apply {
                arguments = Bundle().apply {
                    putString(ARGUMENT_URL, url)
                }
            }
        }
    }

    private var _binding: FragmentOnlineDocsBinding? = null
    private val binding get() = _binding!!
    private lateinit var webView: WebView
    private var indexUrl: String? = null
    private var previousQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        arguments = arguments ?: Bundle()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnlineDocsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = binding.ewebView.webView
        val swipeRefresh = binding.ewebView.swipeRefreshLayout
        swipeRefresh.setOnRefreshListener { handleRefresh() }
        restoreWebViewState(savedInstanceState)
    }

    private fun handleRefresh() {
        if (TextUtils.equals(webView.url, indexUrl)) {
            loadUrl()
        } else {
            binding.ewebView.onRefresh()
        }
    }

    private fun restoreWebViewState(state: Bundle?) {
        val savedState = arguments?.getBundle("savedWebViewState")
        if (savedState != null) {
            webView.restoreState(savedState)
        } else {
            loadUrl()
        }
    }

    private fun loadUrl() {
        val defaultUrl = Pref.getDocumentationUrl() + "index.html"
        indexUrl = arguments?.getString(ARGUMENT_URL, defaultUrl) ?: defaultUrl
        webView.loadUrl(indexUrl.toString())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Bundle().apply {
            webView.saveState(this)
            arguments?.putBundle("savedWebViewState", this)
        }
    }

    @Subscribe
    fun onQuerySubmit(event: QueryEvent) {
        if (!isAdded || !isVisible) return

        when {
            event == QueryEvent.CLEAR -> {
                webView.clearMatches()
                previousQuery = null
            }

            event.isFindForward -> webView.findNext(false)
            event.query == previousQuery -> webView.findNext(true)
            else -> {
                webView.findAllAsync(event.query)
                previousQuery = event.query
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}