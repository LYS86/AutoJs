package org.autojs.autojs.external.tasker;

import static org.autojs.autojs.ui.edit.EditorView.EXTRA_CONTENT;
import static org.autojs.autojs.ui.edit.EditorView.EXTRA_NAME;
import static org.autojs.autojs.ui.edit.EditorView.EXTRA_RUN_ENABLED;
import static org.autojs.autojs.ui.edit.EditorView.EXTRA_SAVE_ENABLED;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.ActivityTaskerScriptEditBinding;
import org.autojs.autojs.timing.TaskReceiver;
import org.autojs.autojs.tool.Observers;
import org.autojs.autojs.ui.BaseActivity;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class TaskerScriptEditActivity extends BaseActivity {
    public static final int REQUEST_CODE = 10016;
    public static final String EXTRA_TASK_ID = TaskReceiver.EXTRA_TASK_ID;
    private ActivityTaskerScriptEditBinding binding;

    public static void edit(Activity activity, String title, String summary, String content) {
        activity.startActivityForResult(new Intent(activity, TaskerScriptEditActivity.class).putExtra(EXTRA_CONTENT, content).putExtra("summary", summary).putExtra(EXTRA_NAME, title), REQUEST_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskerScriptEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupViews();
    }

    @SuppressLint("CheckResult")
    private void setupViews() {
        binding.editorView.handleIntent(getIntent().putExtra(EXTRA_RUN_ENABLED, false).putExtra(EXTRA_SAVE_ENABLED, false)).observeOn(AndroidSchedulers.mainThread()).subscribe(Observers.emptyConsumer(), ex -> {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        });
        BaseActivity.setToolbarAsBack(this, R.id.toolbar, binding.editorView.getName());
    }

    @Override
    public void finish() {
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_CONTENT, binding.editorView.getEditor().getText()));
        super.finish();
    }

    @Override
    protected void onDestroy() {
        binding.editorView.destroy();
        binding = null;
        super.onDestroy();
    }
}