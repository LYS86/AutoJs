package org.autojs.autojs.ui.explorer;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.stardust.autojs.project.ProjectConfig;
import com.stardust.autojs.project.ProjectLauncher;
import com.stardust.pio.PFile;

import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.databinding.ExplorerProjectToolbarBinding;
import org.autojs.autojs.model.explorer.ExplorerChangeEvent;
import org.autojs.autojs.model.explorer.ExplorerItem;
import org.autojs.autojs.model.explorer.Explorers;
import org.autojs.autojs.ui.project.BuildActivity;
import org.autojs.autojs.ui.project.ProjectConfigActivity;
import org.greenrobot.eventbus.Subscribe;

public class ExplorerProjectToolbar extends CardView {

    private ProjectConfig mProjectConfig;
    private PFile mDirectory;
    private ExplorerProjectToolbarBinding binding;

    public ExplorerProjectToolbar(Context context) {
        super(context);
        init();
    }

    public ExplorerProjectToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExplorerProjectToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        binding = ExplorerProjectToolbarBinding.inflate(LayoutInflater.from(getContext()), this, true);
        setupClickListeners();
        setOnClickListener(view -> edit());
    }

    private void setupClickListeners() {
        binding.run.setOnClickListener(v -> run());
        binding.build.setOnClickListener(v -> build());
        binding.sync.setOnClickListener(v -> sync());
    }

    public void setProject(PFile dir) {
        mProjectConfig = ProjectConfig.fromProjectDir(dir.getPath());
        if (mProjectConfig == null) {
            setVisibility(GONE);
            return;
        }
        mDirectory = dir;
        binding.projectName.setText(mProjectConfig.getName());
    }

    public void refresh() {
        if (mDirectory != null) {
            setProject(mDirectory);
        }
    }

    void run() {
        try {
            new ProjectLauncher(mDirectory.getPath())
                    .launch(AutoJs.getInstance().getScriptEngineService());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    void build() {
        Intent intent = new Intent(getContext(), BuildActivity.class);
        intent.putExtra(BuildActivity.EXTRA_SOURCE, mDirectory.getPath());
        getContext().startActivity(intent);
    }

    void sync() {
        // Sync implementation
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Explorers.workspace().registerChangeListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Explorers.workspace().unregisterChangeListener(this);
    }

    @Subscribe
    public void onExplorerChange(ExplorerChangeEvent event) {
        if (mDirectory == null) {
            return;
        }
        ExplorerItem item = event.getItem();
        if ((event.getAction() == ExplorerChangeEvent.ALL)
                || (item != null && mDirectory.getPath().equals(item.getPath()))) {
            refresh();
        }
    }

    void edit() {
        Intent intent = new Intent(getContext(), ProjectConfigActivity.class);
        intent.putExtra(ProjectConfigActivity.EXTRA_DIRECTORY, mDirectory.getPath());
        getContext().startActivity(intent);
    }
}