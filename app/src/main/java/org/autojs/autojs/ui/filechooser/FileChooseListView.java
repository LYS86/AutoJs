package org.autojs.autojs.ui.filechooser;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.stardust.pio.PFile;
import com.stardust.pio.PFiles;

import org.autojs.autojs.databinding.FileChooseListDirectoryBinding;
import org.autojs.autojs.databinding.FileChooseListFileBinding;
import org.autojs.autojs.model.explorer.ExplorerItem;
import org.autojs.autojs.model.explorer.ExplorerPage;
import org.autojs.autojs.model.script.ScriptFile;
import org.autojs.autojs.ui.explorer.ExplorerView;
import org.autojs.autojs.ui.explorer.ExplorerViewHelper;
import org.autojs.autojs.ui.widget.BindableViewHolder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FileChooseListView extends ExplorerView {

    private int mMaxChoice = 1;
    private final LinkedHashMap<PFile, Integer> mSelectedFiles = new LinkedHashMap<>();
    private boolean mCanChooseDir = false;

    public FileChooseListView(Context context) {
        super(context);
        init();
    }

    public FileChooseListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setMaxChoice(int maxChoice) {
        mMaxChoice = maxChoice;
    }

    public void setCanChooseDir(boolean canChooseDir) {
        mCanChooseDir = canChooseDir;
    }

    public List<PFile> getSelectedFiles() {
        return new ArrayList<>(mSelectedFiles.keySet());
    }

    private void init() {
        ((SimpleItemAnimator) getExplorerItemListView().getItemAnimator())
                .setSupportsChangeAnimations(false);
    }

    @Override
    protected BindableViewHolder<?> onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            FileChooseListFileBinding binding = FileChooseListFileBinding.inflate(inflater, parent, false);
            return new ExplorerItemViewHolder(binding);
        } else if (viewType == VIEW_TYPE_PAGE) {
            FileChooseListDirectoryBinding binding = FileChooseListDirectoryBinding.inflate(inflater, parent, false);
            return new ExplorerPageViewHolder(binding);
        }
        return super.onCreateViewHolder(inflater, parent, viewType);
    }

    private void check(ScriptFile file, int position) {
        if (mSelectedFiles.size() == mMaxChoice) {
            Map.Entry<PFile, Integer> entry = mSelectedFiles.entrySet().iterator().next();
            int oldPosition = entry.getValue();
            mSelectedFiles.remove(entry.getKey());
            getExplorerItemListView().getAdapter().notifyItemChanged(oldPosition);
        }
        mSelectedFiles.put(file, position);
    }

    class ExplorerItemViewHolder extends BindableViewHolder<ExplorerItem> {
        private final FileChooseListFileBinding binding;
        private final GradientDrawable firstCharBackground;
        private ExplorerItem explorerItem;

        ExplorerItemViewHolder(FileChooseListFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.firstCharBackground = (GradientDrawable) binding.firstChar.getBackground();

            binding.getRoot().setOnClickListener(v -> binding.checkbox.toggle());
            setupCheckboxListener();
        }

        @Override
        public void bind(ExplorerItem item, int position) {
            explorerItem = item;
            binding.name.setText(ExplorerViewHelper.getDisplayName(item));
            binding.desc.setText(PFiles.getHumanReadableSize(item.getSize()));
            binding.firstChar.setText(ExplorerViewHelper.getIconText(item));
            firstCharBackground.setColor(ExplorerViewHelper.getIconColor(item));

            // Update checkbox state without triggering listener
            binding.checkbox.setOnCheckedChangeListener(null);
            binding.checkbox.setChecked(mSelectedFiles.containsKey(item.toScriptFile()), false);
            setupCheckboxListener();
        }

        private void setupCheckboxListener() {
            binding.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    check(explorerItem.toScriptFile(), getAdapterPosition());
                } else {
                    mSelectedFiles.remove(explorerItem.toScriptFile());
                }
            });
        }
    }

    class ExplorerPageViewHolder extends BindableViewHolder<ExplorerPage> {
        private final FileChooseListDirectoryBinding binding;
        private ExplorerPage explorerPage;

        ExplorerPageViewHolder(FileChooseListDirectoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.checkbox.setVisibility(mCanChooseDir ? VISIBLE : GONE);

            binding.getRoot().setOnClickListener(v -> enterDirectChildPage(explorerPage));
            setupCheckboxListener();
        }

        @Override
        public void bind(ExplorerPage page, int position) {
            explorerPage = page;
            binding.name.setText(ExplorerViewHelper.getDisplayName(page));
            binding.icon.setImageResource(ExplorerViewHelper.getIcon(page));

            if (mCanChooseDir) {
                binding.checkbox.setOnCheckedChangeListener(null);
                binding.checkbox.setChecked(mSelectedFiles.containsKey(page.toScriptFile()), false);
                setupCheckboxListener();
            }
        }

        private void setupCheckboxListener() {
            binding.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    check(explorerPage.toScriptFile(), getAdapterPosition());
                } else {
                    mSelectedFiles.remove(explorerPage.toScriptFile());
                }
            });
        }
    }
}