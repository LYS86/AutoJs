package org.autojs.autojs.ui.log;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;

import com.stardust.autojs.core.console.ConsoleView;
import com.stardust.autojs.core.console.ConsoleImpl;

import org.autojs.autojs.R;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.databinding.ActivityLogBinding;
import org.autojs.autojs.ui.BaseActivity;

public class LogActivity extends BaseActivity {

    ConsoleView mConsoleView;

    private ConsoleImpl mConsoleImpl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyDayNightMode();
        ActivityLogBinding binding = ActivityLogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mConsoleView = binding.console;
        binding.fab.setOnClickListener(v -> clearConsole());
        setupViews();
    }

    void setupViews() {
        setToolbarAsBack(getString(R.string.text_log));
        mConsoleImpl = AutoJs.getInstance().getGlobalConsole();
        mConsoleView.setConsole(mConsoleImpl);
        mConsoleView.findViewById(R.id.input_container).setVisibility(View.GONE);
    }

    void clearConsole() {
        mConsoleImpl.clear();
    }
}
