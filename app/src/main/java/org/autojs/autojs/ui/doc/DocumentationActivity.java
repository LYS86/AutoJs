package org.autojs.autojs.ui.doc;

import android.os.Bundle;
import android.webkit.WebView;

import androidx.annotation.Nullable;

import org.autojs.autojs.Pref;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.ActivityDocumentationBinding;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.widget.EWebView;

/**
 * Created by Stardust on 2017/10/24.
 */
public class DocumentationActivity extends BaseActivity {

    public static final String EXTRA_URL = "url";

    EWebView mEWebView;

    WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityDocumentationBinding binding = ActivityDocumentationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mEWebView = binding.ewebView;
        setUpViews();
    }

    void setUpViews() {
        setToolbarAsBack(getString(R.string.text_tutorial));
        mWebView = mEWebView.getWebView();
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null) {
            url = Pref.getDocumentationUrl() + "index.html";
        }
        mWebView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
