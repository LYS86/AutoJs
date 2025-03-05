package org.autojs.autojs.ui.explorer;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import com.stardust.autojs.project.ProjectConfig;
import com.stardust.autojs.project.ProjectLauncher;
import com.stardust.pio.PFile;

import org.autojs.autojs.R;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.databinding.ExplorerProjectToolbarBinding;
import org.autojs.autojs.model.explorer.ExplorerChangeEvent;
import org.autojs.autojs.model.explorer.ExplorerItem;
import org.autojs.autojs.model.explorer.Explorers;
import org.autojs.autojs.ui.project.BuildActivity;
import org.autojs.autojs.ui.project.BuildActivity_;
import org.autojs.autojs.ui.project.ProjectConfigActivity;
import org.autojs.autojs.ui.project.ProjectConfigActivity_;
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
        BuildActivity_.intent(getContext())
                .extra(BuildActivity.EXTRA_SOURCE, mDirectory.getPath())
                .start();
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
        ProjectConfigActivity_.intent(getContext())
                .extra(ProjectConfigActivity.EXTRA_DIRECTORY, mDirectory.getPath())
                .start();
    }
}