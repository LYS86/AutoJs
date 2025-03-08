package org.autojs.autojs.ui.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.stardust.util.IntentUtil;
import com.tencent.bugly.crashreport.CrashReport;

import org.autojs.autojs.BuildConfig;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.ActivityAboutBinding;
import org.autojs.autojs.databinding.ActivityAboutItemsBinding;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autojs.tool.IntentTool;
import org.autojs.autojs.ui.BaseActivity;

public class AboutActivity extends BaseActivity {

    private static final String TAG = "AboutActivity";
    private ActivityAboutBinding mainBinding;
    private ActivityAboutItemsBinding itemsBinding;
    private int mLolClickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityAboutBinding.inflate(getLayoutInflater());
        View aboutItemsView = mainBinding.getRoot().findViewById(R.id.about_items_root);
        itemsBinding = ActivityAboutItemsBinding.bind(aboutItemsView);
        setContentView(mainBinding.getRoot());

        setUpViews();
        setupClickListeners();
    }

    @SuppressLint("SetTextI18n")
    private void setUpViews() {
        mainBinding.version.setText("Version " + BuildConfig.VERSION_NAME);
        setToolbarAsBack(getString(R.string.text_about));
    }

    private void setupClickListeners() {
        itemsBinding.github.setOnClickListener(v -> openGitHub());
        itemsBinding.qq.setOnClickListener(v -> openQQToChatWithMe());
        itemsBinding.email.setOnClickListener(v -> openEmailToSendMe());

        mainBinding.share.setOnClickListener(v -> share());
        mainBinding.icon.setOnClickListener(v -> lol());
    }

    private void openGitHub() {
        IntentTool.browse(this, getString(R.string.my_github));
    }

    private void openQQToChatWithMe() {
        String qq = getString(R.string.qq);
        if (!IntentUtil.chatWithQQ(this, qq)) {
            Toast.makeText(this, R.string.text_mobile_qq_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private void openEmailToSendMe() {
        String email = getString(R.string.email);
        IntentUtil.sendMailTo(this, email);
    }

    private void share() {
        IntentUtil.shareText(this, getString(R.string.share_app));
    }

    private void lol() {
        mLolClickCount++;
        if (mLolClickCount >= 5) {
            crashTest();
        }
    }

    private void crashTest() {
        new ThemeColorMaterialDialogBuilder(this)
                .title("Crash Test")
                .positiveText("Crash")
                .onPositive((dialog, which) -> CrashReport.testJavaCrash())
                .show();
    }

}