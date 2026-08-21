package org.autojs.autojs.ui.doc;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.FloatingManualDialogBinding;
import org.autojs.autojs.ui.widget.EWebView;

/**
 * Created by Stardust on 2017/10/24.
 */

public class ManualDialog {

    TextView mTitle;

    EWebView mEWebView;

    View mPinToLeft;

    Dialog mDialog;
    private Context mContext;

    public ManualDialog(Context context) {
        mContext = context;
        View view = View.inflate(context, R.layout.floating_manual_dialog, null);
        FloatingManualDialogBinding binding = FloatingManualDialogBinding.bind(view);
        mTitle = binding.title;
        mEWebView = binding.ewebView;
        mPinToLeft = binding.pinToLeft;
        binding.close.setOnClickListener(v -> close());
        binding.fullscreen.setOnClickListener(v -> viewInNewActivity());
        mDialog = new MaterialDialog.Builder(context)
                .customView(view, false)
                .build();
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    public ManualDialog title(String title) {
        mTitle.setText(title);
        return this;
    }

    public ManualDialog url(String url) {
        mEWebView.getWebView().loadUrl(url);
        return this;
    }

    public ManualDialog pinToLeft(View.OnClickListener listener) {
        mPinToLeft.setOnClickListener(v -> {
            mDialog.dismiss();
            listener.onClick(v);
        });
        return this;
    }

    public ManualDialog show() {
        mDialog.show();
        return this;
    }

    void close() {
        mDialog.dismiss();
    }

    void viewInNewActivity() {
        mDialog.dismiss();
        mContext.startActivity(new Intent(mContext, DocumentationActivity.class)
                .putExtra(DocumentationActivity.EXTRA_URL, mEWebView.getWebView().getUrl()));
    }

}
