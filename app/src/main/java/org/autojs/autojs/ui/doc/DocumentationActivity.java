package org.autojs.autojs.ui.doc;

import android.os.Bundle;
import android.webkit.WebView;

import org.autojs.autojs.Pref;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.ActivityDocumentationBinding;
import org.autojs.autojs.ui.BaseActivity;

public class DocumentationActivity extends BaseActivity {

    public static final String EXTRA_URL = "url";
    private ActivityDocumentationBinding binding;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDocumentationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setToolbarAsBack(getString(R.string.text_tutorial));
        mWebView = binding.ewebView.getWebView();
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null) {
            url = Pref.getDocumentationUrl() + "index.html";
        }
        mWebView.loadUrl(url);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
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