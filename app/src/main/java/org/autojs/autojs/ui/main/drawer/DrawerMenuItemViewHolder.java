package org.autojs.autojs.ui.main.drawer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.view.View;
import android.widget.Toast;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.DrawerMenuItemBinding;
import org.autojs.autojs.ui.widget.BindableViewHolder;
import org.autojs.autojs.ui.widget.SwitchCompat;

/**
 * Created by Stardust on 2017/12/10.
 */

public class DrawerMenuItemViewHolder extends BindableViewHolder<DrawerMenuItem> {

    private static final long CLICK_TIMEOUT = 1000;
    private final DrawerMenuItemBinding binding;

    private boolean mAntiShake;
    private long mLastClickMillis;
    private DrawerMenuItem mDrawerMenuItem;

    public DrawerMenuItemViewHolder(DrawerMenuItemBinding binding) {
        super(binding.getRoot());
        this.binding = binding;

        binding.getRoot().setOnClickListener(v -> {
            if (binding.sw.getVisibility() == VISIBLE) {
                binding.sw.toggle();
            } else {
                onClick();
            }
        });
        binding.sw.setOnCheckedChangeListener((buttonView, isChecked) -> onClick());
    }

    @Override
    public void bind(DrawerMenuItem item, int position) {
        mDrawerMenuItem = item;
        binding.icon.setImageResource(item.getIcon());
        binding.title.setText(item.getTitle());
        mAntiShake = item.antiShake();
        setSwitch(item);
        setProgress(item.isProgress());
        setNotifications(item.getNotificationCount());
    }

    private void setNotifications(int notificationCount) {
        if (notificationCount == 0) {
            binding.notifications.setVisibility(View.GONE);
        } else {
            binding.notifications.setVisibility(View.VISIBLE);
            binding.notifications.setText(String.valueOf(notificationCount));
        }
    }

    private void setSwitch(DrawerMenuItem item) {
        if (!item.isSwitchEnabled()) {
            binding.sw.setVisibility(GONE);
            return;
        }
        binding.sw.setVisibility(VISIBLE);
        int prefKey = item.getPrefKey();
        if (prefKey == 0) {
            binding.sw.setChecked(item.isChecked(), false);
            binding.sw.setPrefKey(null);
        } else {
            binding.sw.setPrefKey(itemView.getResources().getString(prefKey));
        }
    }

    private void onClick() {
        mDrawerMenuItem.setChecked(binding.sw.isChecked());
        if (mAntiShake && (System.currentTimeMillis() - mLastClickMillis < CLICK_TIMEOUT)) {
            Toast.makeText(itemView.getContext(), R.string.text_click_too_frequently, Toast.LENGTH_SHORT).show();
            binding.sw.setChecked(!binding.sw.isChecked(), false);
            return;
        }
        mLastClickMillis = System.currentTimeMillis();
        if (mDrawerMenuItem != null) {
            mDrawerMenuItem.performAction(this);
        }
    }

    private void setProgress(boolean onProgress) {
        binding.progressBar.setVisibility(onProgress ? VISIBLE : GONE);
        binding.icon.setVisibility(onProgress ? GONE : VISIBLE);
        binding.sw.setEnabled(!onProgress);
        itemView.setEnabled(!onProgress);
    }

    public SwitchCompat getSwitchCompat() {
        return binding.sw;
    }

}
