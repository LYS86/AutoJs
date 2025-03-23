package org.autojs.autojs.ui.log;

import android.os.Bundle;
import android.view.View;

import com.stardust.autojs.core.console.ConsoleImpl;

import org.autojs.autojs.R;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.databinding.ActivityLogBinding;
import org.autojs.autojs.ui.BaseActivity;

public class LogActivity extends BaseActivity {

    private ActivityLogBinding binding;
    private ConsoleImpl mConsoleImpl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyDayNightMode();
        initBinding();
        setupViews();
    }

    private void initBinding() {
        binding = ActivityLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    private void setupViews() {
        setToolbarAsBack(getString(R.string.text_log));
        initConsole();
        setupFab();
    }

    private void initConsole() {
        mConsoleImpl = AutoJs.getInstance().getGlobalConsole();
        binding.console.setConsole(mConsoleImpl);
        binding.console.findViewById(R.id.input_container).setVisibility(View.GONE);
    }

    private void setupFab() {
        binding.fab.setOnClickListener(v -> mConsoleImpl.clear());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}