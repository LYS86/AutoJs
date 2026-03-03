package org.autojs.autojs.ui.main.drawer

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.app.GlobalAppContext
import com.stardust.app.isOpPermissionGranted
import com.stardust.notification.NotificationListenerService
import com.stardust.util.IntentUtil
import com.stardust.view.accessibility.AccessibilityService
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.databinding.FragmentDrawerBinding
import org.autojs.autojs.external.foreground.ForegroundService
import org.autojs.autojs.network.VersionService
import org.autojs.autojs.network.entity.VersionInfo
import org.autojs.autojs.pluginclient.DevPluginService
import org.autojs.autojs.tool.AccessibilityServiceTool
import org.autojs.autojs.tool.Observers
import org.autojs.autojs.tool.WifiTool
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.common.NotAskAgainDialog
import org.autojs.autojs.ui.floating.CircularMenu
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.settings.SettingsActivity
import org.autojs.autojs.ui.update.UpdateInfoDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.Arrays

class DrawerFragment : androidx.fragment.app.Fragment() {

    private var _binding: FragmentDrawerBinding? = null
    private val binding get() = _binding!!

    private val urlDevPlugin = "https://www.autojs.org/topic/968/"

    private val connectionItem = DrawerMenuItem(R.drawable.ic_connect_to_pc, R.string.debug, 0, this::connectOrDisconnectToRemote)
    private val accessibilityServiceItem = DrawerMenuItem(R.drawable.ic_service_green, R.string.text_accessibility_service, 0, this::enableOrDisableAccessibilityService)
    private val stableModeItem = object : DrawerMenuItem(R.drawable.ic_stable, R.string.text_stable_mode, R.string.key_stable_mode, null) {
        override fun setChecked(checked: Boolean) {
            super.setChecked(checked)
            if (checked) {
                showStableModePromptIfNeeded()
            }
        }
    }

    private val notificationPermissionItem = DrawerMenuItem(R.drawable.ic_ali_notification, R.string.text_notification_permission, 0, this::goToNotificationServiceSettings)
    private val usageStatsPermissionItem = DrawerMenuItem(R.drawable.ic_ali_notification, R.string.text_usage_stats_permission, 0, this::goToUsageStatsSettings)
    private val foregroundServiceItem = DrawerMenuItem(R.drawable.ic_service_green, R.string.text_foreground_service, R.string.key_foreground_servie, this::toggleForegroundService)

    private val floatingWindowItem = DrawerMenuItem(R.drawable.ic_robot_64, R.string.text_floating_window, 0, this::showOrDismissFloatingWindow)
    private val checkForUpdatesItem = DrawerMenuItem(R.drawable.ic_check_for_updates, R.string.text_check_for_updates, this::checkForUpdates)

    private lateinit var drawerMenuAdapter: DrawerMenuAdapter
    private var connectionStateDisposable: io.reactivex.disposables.Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        connectionStateDisposable = DevPluginService.getInstance().connectionState()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { state ->
                connectionItem.let { item ->
                    setChecked(item, state.state == DevPluginService.State.CONNECTED)
                    setProgress(item, state.state == DevPluginService.State.CONNECTING)
                }
                state.exception?.let { showMessage(it.message.orEmpty()) }
            }
        EventBus.getDefault().register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()
    }

    private fun setUpViews() {
        initMenuItems()
        if (Pref.isFloatingMenuShown()) {
            val success = FloatyWindowManger.showCircularMenuIfNeeded()
            setChecked(floatingWindowItem, success)
            if (!success) {
                Pref.setFloatingMenuShown(false)
            }
        }
        setChecked(connectionItem, DevPluginService.getInstance().isConnected())
        if (Pref.isForegroundServiceEnabled()) {
            ForegroundService.start(GlobalAppContext.get())
            setChecked(foregroundServiceItem, true)
        }
    }

    private fun initMenuItems() {
        drawerMenuAdapter = DrawerMenuAdapter(ArrayList(Arrays.asList(
            DrawerMenuGroup(R.string.text_service),
            accessibilityServiceItem,
            stableModeItem,
            notificationPermissionItem,
            foregroundServiceItem,
            usageStatsPermissionItem,

            DrawerMenuGroup(R.string.text_script_record),
            floatingWindowItem,
            DrawerMenuItem(R.drawable.ic_volume, R.string.text_volume_down_control, R.string.key_use_volume_control_record, null),

            DrawerMenuGroup(R.string.text_others),
            connectionItem,
            DrawerMenuItem(R.drawable.ic_personalize, R.string.text_theme_color, this::openThemeColorSettings),
            DrawerMenuItem(R.drawable.ic_night_mode, R.string.text_night_mode, R.string.key_night_mode, this::toggleNightMode),
            checkForUpdatesItem
        )))
        binding.drawerMenu.adapter = drawerMenuAdapter
        binding.drawerMenu.layoutManager = LinearLayoutManager(requireContext())
    }

    fun enableOrDisableAccessibilityService(holder: DrawerMenuItemViewHolder) {
        val isAccessibilityServiceEnabled = isAccessibilityServiceEnabled()
        val checked = holder.getSwitchCompat().isChecked
        if (checked && isAccessibilityServiceEnabled.not()) {
            enableAccessibilityService()
        } else if (checked.not() && isAccessibilityServiceEnabled) {
            if (AccessibilityService.disable().not()) {
                AccessibilityServiceTool.goToAccessibilitySetting()
            }
        }
    }

    fun goToNotificationServiceSettings(holder: DrawerMenuItemViewHolder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return
        }
        val enabled = NotificationListenerService.instance != null
        val checked = holder.getSwitchCompat().isChecked
        if ((checked && enabled.not()) || (checked.not() && enabled)) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    fun goToUsageStatsSettings(holder: DrawerMenuItemViewHolder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return
        }
        val enabled = requireContext().isOpPermissionGranted(AppOpsManager.OPSTR_GET_USAGE_STATS)
        val checked = holder.getSwitchCompat().isChecked
        if (checked && enabled.not()) {
            if (NotAskAgainDialog.Builder(requireContext(), "DrawerFragment.usage_stats")
                .title(R.string.text_usage_stats_permission)
                .content(R.string.description_usage_stats_permission)
                .positiveText(R.string.ok)
                .dismissListener { IntentUtil.requestAppUsagePermission(requireContext()) }
                .show() == null) {
                IntentUtil.requestAppUsagePermission(requireContext())
            }
        }
        if (checked.not() && enabled) {
            IntentUtil.requestAppUsagePermission(requireContext())
        }
    }

    fun showOrDismissFloatingWindow(holder: DrawerMenuItemViewHolder) {
        val isFloatingWindowShowing = FloatyWindowManger.isCircularMenuShowing()
        val checked = holder.getSwitchCompat().isChecked
        if (checked && isFloatingWindowShowing.not()) {
            val success = FloatyWindowManger.showCircularMenu()
            setChecked(floatingWindowItem, success)
            Pref.setFloatingMenuShown(success)
            if (success) {
                enableAccessibilityServiceByRootIfNeeded()
            }
        } else if (checked.not() && isFloatingWindowShowing) {
            FloatyWindowManger.hideCircularMenu()
            Pref.setFloatingMenuShown(false)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun openThemeColorSettings(_holder: DrawerMenuItemViewHolder) {
        activity?.let { SettingsActivity.selectThemeColor(it) }
    }

    fun toggleNightMode(holder: DrawerMenuItemViewHolder) {
        (activity as? BaseActivity)?.setNightModeEnabled(holder.getSwitchCompat().isChecked)
    }

    @SuppressLint("CheckResult")
    private fun enableAccessibilityServiceByRootIfNeeded() {
        Observable.fromCallable { Pref.shouldEnableAccessibilityServiceByRoot() && isAccessibilityServiceEnabled().not() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { needed ->
                if (needed) {
                    enableAccessibilityServiceByRoot()
                }
            }
    }

    fun connectOrDisconnectToRemote(holder: DrawerMenuItemViewHolder) {
        val checked = holder.getSwitchCompat().isChecked
        val connected = DevPluginService.getInstance().isConnected()
        if (checked && connected.not()) {
            inputRemoteHost()
        } else if (checked.not() && connected) {
            DevPluginService.getInstance().disconnectIfNeeded()
        }
    }

    private fun toggleForegroundService(holder: DrawerMenuItemViewHolder) {
        val checked = holder.getSwitchCompat().isChecked
        if (checked) {
            ForegroundService.start(GlobalAppContext.get())
        } else {
            ForegroundService.stop(GlobalAppContext.get())
        }
    }

    private fun inputRemoteHost() {
        val host = Pref.getServerAddressOrDefault(WifiTool.getRouterIp(activity))
        MaterialDialog.Builder(requireActivity())
            .title(R.string.text_server_address)
            .input("", host) { _, input ->
                Pref.saveServerAddress(input.toString())
                DevPluginService.getInstance().connectToServer(input.toString())
                    .subscribe(Observers.emptyConsumer(), this::onConnectException)
            }
            .neutralText(R.string.text_help)
            .onNeutral { _, _ ->
                setChecked(connectionItem, false)
                IntentUtil.browse(activity, urlDevPlugin)
            }
            .cancelListener { setChecked(connectionItem, false) }
            .show()
    }

    private fun onConnectException(e: Throwable) {
        setChecked(connectionItem, false)
        Toast.makeText(GlobalAppContext.get(), getString(R.string.error_connect_to_remote, e.message), Toast.LENGTH_LONG).show()
    }

    @Suppress("UNUSED_PARAMETER")
    @SuppressLint("CheckResult")
    fun checkForUpdates(_holder: DrawerMenuItemViewHolder) {
        setProgress(checkForUpdatesItem, true)
        VersionService.getInstance().checkForUpdates()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : io.reactivex.Observer<VersionInfo> {
                override fun onSubscribe(d: io.reactivex.disposables.Disposable) {}

                override fun onNext(versionInfo: VersionInfo) {
                    if (activity == null) return
                    if (versionInfo.isNewer) {
                        UpdateInfoDialogBuilder(requireActivity(), versionInfo).show()
                    } else {
                        Toast.makeText(GlobalAppContext.get(), R.string.text_is_latest_version, Toast.LENGTH_SHORT).show()
                    }
                    setProgress(checkForUpdatesItem, false)
                }

                override fun onError(e: Throwable) {
                    e.printStackTrace()
                    Toast.makeText(GlobalAppContext.get(), R.string.text_check_update_error, Toast.LENGTH_SHORT).show()
                    setProgress(checkForUpdatesItem, false)
                }

                override fun onComplete() {}
            })
    }

    override fun onResume() {
        super.onResume()
        syncSwitchState()
    }

    private fun syncSwitchState() {
        setChecked(accessibilityServiceItem, AccessibilityServiceTool.isAccessibilityServiceEnabled(activity))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setChecked(notificationPermissionItem, NotificationListenerService.instance != null)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setChecked(usageStatsPermissionItem, requireContext().isOpPermissionGranted(AppOpsManager.OPSTR_GET_USAGE_STATS))
        }
    }

    private fun enableAccessibilityService() {
        if (Pref.shouldEnableAccessibilityServiceByRoot().not()) {
            AccessibilityServiceTool.goToAccessibilitySetting()
            return
        }
        enableAccessibilityServiceByRoot()
    }

    @SuppressLint("CheckResult")
    private fun enableAccessibilityServiceByRoot() {
        setProgress(accessibilityServiceItem, true)
        Observable.fromCallable { AccessibilityServiceTool.enableAccessibilityServiceByRootAndWaitFor(4000) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { succeed ->
                if (succeed.not()) {
                    Toast.makeText(context, R.string.text_enable_accessibitliy_service_by_root_failed, Toast.LENGTH_SHORT).show()
                    AccessibilityServiceTool.goToAccessibilitySetting()
                }
                setProgress(accessibilityServiceItem, false)
            }
    }

    @Subscribe
    fun onCircularMenuStateChange(event: CircularMenu.StateChangeEvent) {
        setChecked(floatingWindowItem, event.currentState != CircularMenu.STATE_CLOSED)
    }

    private fun showStableModePromptIfNeeded() {
        NotAskAgainDialog.Builder(requireContext(), "DrawerFragment.stable_mode")
            .title(R.string.text_stable_mode)
            .content(R.string.description_stable_mode)
            .positiveText(R.string.ok)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionStateDisposable?.dispose()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showMessage(text: CharSequence) {
        context?.let { Toast.makeText(it, text, Toast.LENGTH_SHORT).show() }
    }

    private fun setProgress(item: DrawerMenuItem, progress: Boolean) {
        item.setProgress(progress)
        drawerMenuAdapter.notifyItemChanged(item)
    }

    private fun setChecked(item: DrawerMenuItem, checked: Boolean) {
        item.setChecked(checked)
        drawerMenuAdapter.notifyItemChanged(item)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return AccessibilityServiceTool.isAccessibilityServiceEnabled(activity)
    }
}
