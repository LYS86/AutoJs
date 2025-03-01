package org.autojs.autojs.ui.edit;

import static org.autojs.autojs.model.script.Scripts.ACTION_ON_EXECUTION_FINISHED;
import static org.autojs.autojs.model.script.Scripts.EXTRA_EXCEPTION_MESSAGE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.stardust.app.OnActivityResultDelegate;
import com.stardust.autojs.engine.JavaScriptEngine;
import com.stardust.autojs.execution.ScriptExecution;
import com.stardust.theme.ThemeColorManager;
import com.stardust.util.SparseArrayEntries;

import org.autojs.autojs.R;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.databinding.ActivityViewSampleBinding;
import org.autojs.autojs.model.sample.SampleFile;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.common.ScriptOperations;
import org.autojs.autojs.ui.widget.ToolbarMenuItem;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class ViewSampleActivity extends AppCompatActivity implements OnActivityResultDelegate.DelegateHost {

    private ActivityViewSampleBinding binding;
    private SampleFile mSample;
    private ScriptExecution mScriptExecution;
    private SparseArray<ToolbarMenuItem> mMenuMap;
    private final OnActivityResultDelegate.Mediator mMediator = new OnActivityResultDelegate.Mediator();
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private final BroadcastReceiver mOnRunFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_ON_EXECUTION_FINISHED)) {
                mScriptExecution = null;
                String msg = intent.getStringExtra(EXTRA_EXCEPTION_MESSAGE);
                if (msg != null) {
                    Snackbar.make(binding.getRoot(), getString(R.string.text_error) + ": " + msg, Snackbar.LENGTH_LONG).show();
                }
            }
        }
    };

    public static void view(Context context, SampleFile sample) {
        context.startActivity(new Intent(context, ViewSampleActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("sample_path", sample.getPath()));
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        binding = ActivityViewSampleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        handleIntent(getIntent());
        setUpUI();
        registerReceiver(mOnRunFinishedReceiver, new IntentFilter(ACTION_ON_EXECUTION_FINISHED));
    }

    private void handleIntent(Intent intent) {
        mSample = new SampleFile(intent.getStringExtra("sample_path"), getAssets());
    }

    private void setUpUI() {
        ThemeColorManager.addActivityStatusBar(this);
        setUpToolbar();
        initMenuItem();
        setupClickListeners();
    }

    private void setUpToolbar() {
        BaseActivity.setToolbarAsBack(this, R.id.toolbar, mSample.getSimplifiedName());
    }

    private void setupClickListeners() {
        binding.run.setOnClickListener(v -> run());
        binding.edit.setOnClickListener(v -> edit());
    }

    private void run() {
        Snackbar.make(binding.getRoot(), R.string.text_start_running, Snackbar.LENGTH_SHORT).show();
    }

    private void edit() {
        mCompositeDisposable.add(
                new ScriptOperations(this, binding.getRoot())
                        .importSample(mSample)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(path -> {
                            EditActivity.editFile(ViewSampleActivity.this, path, false);
                            finish();
                        })
        );
    }

    private void initMenuItem() {
        mMenuMap = new SparseArrayEntries<ToolbarMenuItem>().entry(R.id.run, binding.run).sparseArray();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_console) {
            showConsole();
            return true;
        } else if (id == R.id.action_log) {
            showLog();
            return true;
        } else if (id == R.id.action_help) {
            return true;
        } else if (id == R.id.action_import) {
            mCompositeDisposable.add(
                    new ScriptOperations(this, binding.getRoot())
                            .importSample(mSample)
                            .subscribe()
            );
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLog() {
        AutoJs.getInstance().getScriptEngineService().getGlobalConsole().show();
    }

    private void showConsole() {
        if (mScriptExecution != null) {
            ((JavaScriptEngine) mScriptExecution.getEngine()).getRuntime().console.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.clear();
        binding = null;
        unregisterReceiver(mOnRunFinishedReceiver);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            super.onRestoreInstanceState(savedInstanceState);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public OnActivityResultDelegate.Mediator getOnActivityResultDelegateMediator() {
        return mMediator;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mMediator.onActivityResult(requestCode, resultCode, data);
    }
}