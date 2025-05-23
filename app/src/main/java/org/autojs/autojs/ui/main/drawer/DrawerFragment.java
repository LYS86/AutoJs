package org.autojs.autojs.ui.main.drawer;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.stardust.app.AppOpsKt;
import com.stardust.app.GlobalAppContext;
import com.stardust.notification.NotificationListenerService;
import com.stardust.util.IntentUtil;
import com.stardust.view.accessibility.AccessibilityService;

import org.autojs.autojs.Pref;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.FragmentDrawerBinding;
import org.autojs.autojs.external.foreground.ForegroundService;
import org.autojs.autojs.pluginclient.DevPluginService;
import org.autojs.autojs.tool.AccessibilityServiceTool;
import org.autojs.autojs.tool.Observers;
import org.autojs.autojs.tool.WifiTool;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.common.NotAskAgainDialog;
import org.autojs.autojs.ui.floating.CircularMenu;
import org.autojs.autojs.ui.floating.FloatyWindowManger;
import org.autojs.autojs.ui.settings.SettingsActivity;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DrawerFragment extends androidx.fragment.app.Fragment {

    private static final String URL_DEV_PLUGIN = "https://www.autojs.org/topic/968/";
    private final DrawerMenuItem mStableModeItem = new DrawerMenuItem(R.drawable.ic_stable, R.string.text_stable_mode, R.string.key_stable_mode, null) {
        @Override
        public void setChecked(boolean checked) {
            super.setChecked(checked);
            if (checked) showStableModePromptIfNeeded();
        }
    };
    private final DrawerMenuItem mNotificationPermissionItem = new DrawerMenuItem(R.drawable.ic_ali_notification, R.string.text_notification_permission, 0, this::goToNotificationServiceSettings);
    private final DrawerMenuItem mForegroundServiceItem = new DrawerMenuItem(R.drawable.ic_service_green, R.string.text_foreground_service, R.string.key_foreground_servie, this::toggleForegroundService);
    private final DrawerMenuItem mUsageStatsPermissionItem = new DrawerMenuItem(R.drawable.ic_ali_notification, R.string.text_usage_stats_permission, 0, this::goToUsageStatsSettings);
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Disposable mConnectionStateDisposable;
    private FragmentDrawerBinding binding;
    private MaterialDialog remoteHostDialog;
    private DrawerMenuAdapter mDrawerMenuAdapter;

    private void inputRemoteHost() {
        String host = Pref.getServerAddressOrDefault(WifiTool.getRouterIp(getActivity()));
        remoteHostDialog = new MaterialDialog.Builder(getActivity()).title(R.string.text_server_address).input("", host, (dialog, input) -> {
            Pref.saveServerAddress(input.toString());
            Disposable disposable = DevPluginService.getInstance().connectToServer(input.toString()).subscribe(Observers.emptyConsumer(), this::onConnectException);
            compositeDisposable.add(disposable);
        }).neutralText(R.string.text_help).onNeutral((dialog, which) -> {
            setChecked(mConnectionItem, false);
            IntentUtil.browse(getActivity(), URL_DEV_PLUGIN);
        }).cancelListener(dialog -> setChecked(mConnectionItem, false)).show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initMenuItems();
        if (Pref.isFloatingMenuShown()) {
            FloatyWindowManger.showCircularMenuIfNeeded();
            setChecked(mFloatingWindowItem, true);
        }
        setChecked(mConnectionItem, DevPluginService.getInstance().isConnected());
        if (Pref.isForegroundServiceEnabled()) {
            ForegroundService.start(GlobalAppContext.get());
            setChecked(mForegroundServiceItem, true);
        }
        binding.drawerMenu.setAdapter(mDrawerMenuAdapter);
        binding.drawerMenu.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.setting.setOnClickListener(v -> startActivity(new Intent(getActivity(), SettingsActivity.class)));
        binding.exit.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().finishAffinity();
                System.exit(0);
            }
        });
        if (Pref.isConnected() && !DevPluginService.getInstance().isConnected()) {
            String host = Pref.getServerAddressOrDefault(WifiTool.getRouterIp(getActivity()));
            Disposable disposable = DevPluginService.getInstance()
                    .connectToServer(host)
                    .subscribe(Observers.emptyConsumer(), this::onConnectException);
            compositeDisposable.add(disposable);
        }
    }

    @SuppressLint("CheckResult")
    private void enableAccessibilityServiceByRootIfNeeded() {
        Disposable disposable = Observable.fromCallable(() -> Pref.shouldEnableAccessibilityServiceByRoot() && !isAccessibilityServiceEnabled()).observeOn(AndroidSchedulers.mainThread()).subscribe(needed -> {
            if (needed) {
                enableAccessibilityServiceByRoot();
            }
        });
        compositeDisposable.add(disposable);
    }
@Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDrawerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }private final DrawerMenuItem mCheckForUpdatesItem = new DrawerMenuItem(R.drawable.ic_check_for_updates, R.string.text_check_for_updates, this::checkForUpdates);

    private void enableAccessibilityServiceByRoot() {
        setProgress(mAccessibilityServiceItem, true);
        Disposable disposable = Observable.fromCallable(() -> AccessibilityServiceTool.enableAccessibilityServiceByRootAndWaitFor(4000)).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(succeed -> {
            if (!succeed) {
                Toast.makeText(getContext(), R.string.text_enable_accessibitliy_service_by_root_failed, Toast.LENGTH_SHORT).show();
                AccessibilityServiceTool.goToAccessibilitySetting();
            }
            setProgress(mAccessibilityServiceItem, false);
        });
        compositeDisposable.add(disposable);
    }

    void goToUsageStatsSettings(DrawerMenuItemViewHolder holder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        boolean enabled = AppOpsKt.isOpPermissionGranted(getContext(), AppOpsManager.OPSTR_GET_USAGE_STATS);
        boolean checked = holder.getSwitchCompat().isChecked();
        if (checked && !enabled) {
            if (new NotAskAgainDialog.Builder(getContext(), "DrawerFragment.usage_stats").title(R.string.text_usage_stats_permission).content(R.string.description_usage_stats_permission).positiveText(R.string.ok).dismissListener(dialog -> IntentUtil.requestAppUsagePermission(getContext())).show() == null) {
                IntentUtil.requestAppUsagePermission(getContext());
            }
        }
        if (!checked && enabled) {
            IntentUtil.requestAppUsagePermission(getContext());
        }
    }

    void checkForUpdates(DrawerMenuItemViewHolder holder) {
//        TODO 关闭更新功能，等待更新，未来使用github api进行更新
        setProgress(mCheckForUpdatesItem, true);
        showMessage("功能维护中");
        setProgress(mCheckForUpdatesItem, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConnectionStateDisposable = DevPluginService.getInstance().connectionState().observeOn(AndroidSchedulers.mainThread()).subscribe(state -> {
            if (mConnectionItem != null) {
                setChecked(mConnectionItem, state.getState() == DevPluginService.State.CONNECTED);
                setProgress(mConnectionItem, state.getState() == DevPluginService.State.CONNECTING);
            }
            if (state.getException() != null) {
                Pref.setConnected(false);
                showMessage(state.getException().getMessage());
            }
        });
        EventBus.getDefault().register(this);

    }

        private void initMenuItems() {
        mDrawerMenuAdapter = new DrawerMenuAdapter(new ArrayList<>(Arrays.asList(new DrawerMenuGroup(R.string.text_service), mAccessibilityServiceItem, mStableModeItem, mNotificationPermissionItem, mForegroundServiceItem, mUsageStatsPermissionItem,
                new DrawerMenuGroup(R.string.text_script_record),
                mFloatingWindowItem,
                new DrawerMenuItem(R.drawable.ic_volume, R.string.text_volume_down_control, R.string.key_use_volume_control_record, null),
                new DrawerMenuGroup(R.string.text_others), mConnectionItem,
                new DrawerMenuItem(R.drawable.ic_personalize, R.string.text_theme_color, this::openThemeColorSettings),
                new DrawerMenuItem(R.drawable.ic_night_mode, R.string.text_night_mode, R.string.key_night_mode, this::toggleNightMode), mCheckForUpdatesItem)));
    }
@Override
    public void onDestroyView() {
        super.onDestroyView();
        if (remoteHostDialog != null && remoteHostDialog.isShowing()) {
            remoteHostDialog.dismiss();
        }
        binding = null;
    }private final DrawerMenuItem mConnectionItem = new DrawerMenuItem(R.drawable.ic_connect_to_pc, R.string.debug, 0, this::connectOrDisconnectToRemote);

    private void onConnectException(Throwable e) {
        setChecked(mConnectionItem, false);
        Pref.setConnected(false);
        Toast.makeText(GlobalAppContext.get(), getString(R.string.error_connect_to_remote, e.getMessage()), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mConnectionStateDisposable.dispose();
        compositeDisposable.clear();
        EventBus.getDefault().unregister(this);
    }

    void goToNotificationServiceSettings(DrawerMenuItemViewHolder holder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return;
        }
        boolean enabled = NotificationListenerService.Companion.getInstance() != null;
        boolean checked = holder.getSwitchCompat().isChecked();
        if ((checked && !enabled) || (!checked && enabled)) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }    private final DrawerMenuItem mFloatingWindowItem = new DrawerMenuItem(R.drawable.ic_robot_64, R.string.text_floating_window, 0, this::showOrDismissFloatingWindow);



    void enableOrDisableAccessibilityService(DrawerMenuItemViewHolder holder) {
        boolean isAccessibilityServiceEnabled = isAccessibilityServiceEnabled();
        boolean checked = holder.getSwitchCompat().isChecked();
        if (checked && !isAccessibilityServiceEnabled) {
            enableAccessibilityService();
        } else if (!checked && isAccessibilityServiceEnabled) {
            if (!AccessibilityService.Companion.disable()) {
                AccessibilityServiceTool.goToAccessibilitySetting();
            }
        }
    }

        private final DrawerMenuItem mAccessibilityServiceItem = new DrawerMenuItem(R.drawable.ic_service_green, R.string.text_accessibility_service, 0, this::enableOrDisableAccessibilityService);

    void showOrDismissFloatingWindow(DrawerMenuItemViewHolder holder) {
        boolean isFloatingWindowShowing = FloatyWindowManger.isCircularMenuShowing();
        boolean checked = holder.getSwitchCompat().isChecked();
        if (getActivity() != null && !getActivity().isFinishing()) {
            Pref.setFloatingMenuShown(checked);
        }
        if (checked && !isFloatingWindowShowing) {
            setChecked(mFloatingWindowItem, FloatyWindowManger.showCircularMenu());
            enableAccessibilityServiceByRootIfNeeded();
        } else if (!checked && isFloatingWindowShowing) {
            FloatyWindowManger.hideCircularMenu();
        }
    }

    void openThemeColorSettings(DrawerMenuItemViewHolder holder) {
        SettingsActivity.selectThemeColor(getActivity());
    }

    void toggleNightMode(DrawerMenuItemViewHolder holder) {
        ((BaseActivity) getActivity()).setNightModeEnabled(holder.getSwitchCompat().isChecked());
    }

    void connectOrDisconnectToRemote(DrawerMenuItemViewHolder holder) {
        boolean checked = holder.getSwitchCompat().isChecked();
        boolean connected = DevPluginService.getInstance().isConnected();
        if (checked && !connected) {
            inputRemoteHost();
        } else if (!checked && connected) {
            DevPluginService.getInstance().disconnectIfNeeded();
        }
    }

    private void toggleForegroundService(DrawerMenuItemViewHolder holder) {
        boolean checked = holder.getSwitchCompat().isChecked();
        if (checked) {
            ForegroundService.start(GlobalAppContext.get());
        } else {
            ForegroundService.stop(GlobalAppContext.get());
        }
    }

    private void showStableModePromptIfNeeded() {
        new NotAskAgainDialog.Builder(getContext(), "DrawerFragment.stable_mode").title(R.string.text_stable_mode).content(R.string.description_stable_mode).positiveText(R.string.ok).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncSwitchState();
    }

    private void syncSwitchState() {
        setChecked(mAccessibilityServiceItem, AccessibilityServiceTool.isAccessibilityServiceEnabled(getActivity()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setChecked(mNotificationPermissionItem, NotificationListenerService.Companion.getInstance() != null);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setChecked(mUsageStatsPermissionItem, AppOpsKt.isOpPermissionGranted(getContext(), AppOpsManager.OPSTR_GET_USAGE_STATS));
        }
    }

    private void enableAccessibilityService() {
        if (!Pref.shouldEnableAccessibilityServiceByRoot()) {
            AccessibilityServiceTool.goToAccessibilitySetting();
            return;
        }
        enableAccessibilityServiceByRoot();
    }

    @Subscribe
    public void onCircularMenuStateChange(CircularMenu.StateChangeEvent event) {
        setChecked(mFloatingWindowItem, event.getCurrentState() != CircularMenu.STATE_CLOSED);
    }

    private void showMessage(CharSequence text) {
        if (getContext() == null) return;
        Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
    }

    private void setProgress(DrawerMenuItem item, boolean progress) {
        item.setProgress(progress);
        mDrawerMenuAdapter.notifyItemChanged(item);
    }

    private void setChecked(DrawerMenuItem item, boolean checked) {
        item.setChecked(checked);
        mDrawerMenuAdapter.notifyItemChanged(item);
    }

    private boolean isAccessibilityServiceEnabled() {
        return AccessibilityServiceTool.isAccessibilityServiceEnabled(getActivity());
    }










}
