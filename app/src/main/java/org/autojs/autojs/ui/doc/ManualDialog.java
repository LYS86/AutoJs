package org.autojs.autojs.ui.doc;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.FloatingManualDialogBinding;
import org.autojs.autojs.ui.widget.EWebView;

public class ManualDialog {

    private final FloatingManualDialogBinding binding;
    private final Dialog mDialog;
    private final Context mContext;

    public ManualDialog(Context context) {
        mContext = context;
        binding = FloatingManualDialogBinding.inflate(LayoutInflater.from(context));

        mDialog = new MaterialDialog.Builder(context)
                .customView(binding.getRoot(), false)
                .build();
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.close.setOnClickListener(v -> close());
        binding.fullscreen.setOnClickListener(v -> viewInNewActivity());
    }

    public ManualDialog title(String title) {
        binding.title.setText(title);
        return this;
    }

    public ManualDialog url(String url) {
        binding.ewebView.getWebView().loadUrl(url);
        return this;
    }

    public ManualDialog pinToLeft(View.OnClickListener listener) {
        binding.pinToLeft.setOnClickListener(v -> {
            mDialog.dismiss();
            listener.onClick(v);
        });
        return this;
    }

    public ManualDialog show() {
        mDialog.show();
        return this;
    }

    private void close() {
        mDialog.dismiss();
    }

    private void viewInNewActivity() {
        mDialog.dismiss();
        DocumentationActivity_.intent(mContext)
                .extra(DocumentationActivity.EXTRA_URL, binding.ewebView.getWebView().getUrl())
                .start();
    }
}