package org.autojs.autojs.ui.doc

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.stardust.util.BackPressedHandler
import org.autojs.autojs.Pref
import org.autojs.autojs.databinding.FragmentOnlineDocsBinding
import org.autojs.autojs.ui.main.QueryEvent
import org.autojs.autojs.ui.main.ViewPagerFragment
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class DocsFragment : ViewPagerFragment, BackPressedHandler {

    companion object {
        const val ARGUMENT_URL = "url"
    }

    private var _binding: FragmentOnlineDocsBinding? = null
    private val binding get() = _binding!!

    private var webView: WebView? = null
    private var indexUrl: String? = null
    private var previousQuery: String? = null

    constructor() : super(ROTATION_GONE) {
        arguments = Bundle()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): android.view.View {
        _binding = FragmentOnlineDocsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()
    }

    private fun setUpViews() {
        webView = binding.ewebView.webView
        binding.ewebView.swipeRefreshLayout.setOnRefreshListener {
            if (webView?.url == indexUrl) {
                loadUrl()
            } else {
                binding.ewebView.onRefresh()
            }
        }
        val savedWebViewState = arguments?.getBundle("savedWebViewState")
        if (savedWebViewState != null) {
            webView?.restoreState(savedWebViewState)
        } else {
            loadUrl()
        }
    }

    private fun loadUrl() {
        indexUrl = arguments?.getString(ARGUMENT_URL) ?: (Pref.getDocumentationUrl() + "index.html")
        webView?.loadUrl(indexUrl!!)
    }

    override fun onPause() {
        super.onPause()
        val savedWebViewState = Bundle()
        webView?.saveState(savedWebViewState)
        arguments?.putBundle("savedWebViewState", savedWebViewState)
    }

    override fun onBackPressed(activity: Activity): Boolean {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
            return true
        }
        return false
    }

    override fun onFabClick(fab: FloatingActionButton) {
    }

    @Subscribe
    fun onQuerySummit(event: QueryEvent) {
        if (isShown.not()) return
        if (event == QueryEvent.CLEAR) {
            webView?.clearMatches()
            previousQuery = null
            return
        }
        if (event.isFindForward) {
            webView?.findNext(false)
            return
        }
        if (event.query == previousQuery) {
            webView?.findNext(true)
            return
        }
        webView?.findAllAsync(event.query)
        previousQuery = event.query
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webView = null
        _binding = null
    }
}
