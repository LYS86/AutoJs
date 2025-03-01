package org.autojs.autojs.ui.doc;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.stardust.util.BackPressedHandler;

import org.autojs.autojs.Pref;
import org.autojs.autojs.databinding.FragmentOnlineDocsBinding;
import org.autojs.autojs.ui.main.QueryEvent;
import org.autojs.autojs.ui.main.ViewPagerFragment;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class DocsFragment extends ViewPagerFragment implements BackPressedHandler {

    public static final String ARGUMENT_URL = "url";

    private FragmentOnlineDocsBinding binding;
    private WebView mWebView;
    private String mIndexUrl;
    private String mPreviousQuery;

    public DocsFragment() {
        super(ROTATION_GONE);
    }

    public static DocsFragment newInstance(String url) {
        Bundle args = new Bundle();
        args.putString(ARGUMENT_URL, url);
        DocsFragment fragment = new DocsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setArguments(getArguments() != null ? getArguments() : new Bundle());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOnlineDocsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mWebView = binding.ewebView.getWebView();
        SwipeRefreshLayout swipeRefresh = binding.ewebView.getSwipeRefreshLayout();
        swipeRefresh.setOnRefreshListener(this::handleRefresh);
        restoreWebViewState(savedInstanceState);
    }

    private void handleRefresh() {
        if (TextUtils.equals(mWebView.getUrl(), mIndexUrl)) {
            loadUrl();
        } else {
            binding.ewebView.onRefresh();
        }
    }

    private void restoreWebViewState(Bundle state) {
        Bundle savedState = getArguments().getBundle("savedWebViewState");
        if (savedState != null) {
            mWebView.restoreState(savedState);
        } else {
            loadUrl();
        }
    }

    private void loadUrl() {
        Bundle args = getArguments();
        String defaultUrl = Pref.getDocumentationUrl() + "index.html";
        mIndexUrl = args.getString(ARGUMENT_URL, defaultUrl);
        mWebView.loadUrl(mIndexUrl);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle webViewState = new Bundle();
        mWebView.saveState(webViewState);
        getArguments().putBundle("savedWebViewState", webViewState);
    }

    @Override
    public boolean onBackPressed(Activity activity) {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return false;
    }

    @Override
    protected void onFabClick(FloatingActionButton fab) {
    }

    @Subscribe
    public void onQuerySummit(QueryEvent event) {
        if (!isAdded() || !isVisible()) return;

        if (event == QueryEvent.CLEAR) {
            mWebView.clearMatches();
            mPreviousQuery = null;
            return;
        }
        handleSearchQuery(event);
    }

    private void handleSearchQuery(QueryEvent event) {
        if (event.isFindForward()) {
            mWebView.findNext(false);
            return;
        }
        if (event.getQuery().equals(mPreviousQuery)) {
            mWebView.findNext(true);
            return;
        }
        mWebView.findAllAsync(event.getQuery());
        mPreviousQuery = event.getQuery();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}