package org.autojs.autojs.ui.main.drawer

import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.permission.PermissionManager
import com.stardust.autojs.shizuku.Shell
import com.stardust.autojs.util.AccessibilityServiceUtils
import com.stardust.autojs.util.Browser
import com.stardust.enhancedfloaty.FloatyService
import com.stardust.notification.NotificationListenerService
import com.stardust.util.IntentUtil
import com.stardust.view.accessibility.AccessibilityService
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.launch
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.databinding.FragmentDrawerBinding
import org.autojs.autojs.external.foreground.ForegroundService
import org.autojs.autojs.pluginclient.DevPluginService
import org.autojs.autojs.theme.ThemeUtils
import org.autojs.autojs.tool.Observers
import org.autojs.autojs.tool.WifiTool
import org.autojs.autojs.ui.common.NotAskAgainDialog
import org.autojs.autojs.ui.floating.CircularMenu
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.compose.settings.SettingsActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import rikka.shizuku.Shizuku

class DrawerFragment : Fragment() {

    private var _binding: FragmentDrawerBinding? = null
    private val binding get() = _binding!!

    private val urlDevPlugin = "https://www.autojs.org/topic/968/"
    private val REQUEST_CODE_SHIZUKU = 1001

    private val connectionItem =
        DrawerMenuItem(R.drawable.ic_connect_to_pc, R.string.debug, 0, this::connectOrDisconnectToRemote)
    private val accessibilityServiceItem = DrawerMenuItem(
        R.drawable.ic_service_green,
        R.string.text_accessibility_service,
        0,
        this::enableOrDisableAccessibilityService
    )
    private val stableModeItem =
        object : DrawerMenuItem(R.drawable.ic_stable, R.string.text_stable_mode, R.string.key_stable_mode, null) {
            override fun setChecked(checked: Boolean) {
                super.setChecked(checked)
                if (checked) {
                    showStableModePromptIfNeeded()
                }
            }
        }

    private val notificationPermissionItem = DrawerMenuItem(
        R.drawable.ic_ali_notification,
        R.string.text_notification_permission,
        0,
        this::goToNotificationServiceSettings
    )
    private val usageStatsPermissionItem = DrawerMenuItem(
        R.drawable.ic_ali_notification,
        R.string.text_usage_stats_permission,
        0,
        this::goToUsageStatsSettings
    )
    private val foregroundServiceItem = DrawerMenuItem(
        R.drawable.ic_service_green,
        R.string.text_foreground_service,
        R.string.key_foreground_servie,
        this::toggleForegroundService
    )

    private val shizukuItem = DrawerMenuItem(
        R.drawable.ic_service_green,
        R.string.text_shizuku,
        0,
        this::onShizukuToggle
    )

    private val floatingWindowItem =
        DrawerMenuItem(R.drawable.ic_robot_64, R.string.text_floating_window, 0, this::showOrDismissFloatingWindow)
    private val checkForUpdatesItem =
        DrawerMenuItem(R.drawable.ic_check_for_updates, R.string.text_check_for_updates, this::checkForUpdates)

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
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    private fun setUpViews() {
        initMenuItems()
        setUpBottomButtons()
        if (Pref.isFloatingMenuShown()) {
            val success = FloatyWindowManger.showCircularMenuIfNeeded()
            setChecked(floatingWindowItem, success)
            if (!success) {
                Pref.setFloatingMenuShown(false)
            }
        }
        setChecked(connectionItem, DevPluginService.getInstance().isConnected())
        if (Pref.isForegroundServiceEnabled()) {
            startForegroundService()
        }
    }

    private fun setUpBottomButtons() {
        binding.setting.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        binding.exit.setOnClickListener {
            exitCompletely()
        }
    }

    private fun exitCompletely() {
        requireActivity().finish()
        FloatyWindowManger.hideCircularMenu()
        ForegroundService.stop(requireContext())
        requireContext().stopService(Intent(requireContext(), FloatyService::class.java))
        AutoJs.getInstance().scriptEngineService.stopAll()
    }

    private fun initMenuItems() {
        drawerMenuAdapter = DrawerMenuAdapter(
            listOf(
                DrawerMenuGroup(R.string.text_service),
                accessibilityServiceItem,
                stableModeItem,
                notificationPermissionItem,
                foregroundServiceItem,
                shizukuItem,
                usageStatsPermissionItem,

                DrawerMenuGroup(R.string.text_script_record),
                floatingWindowItem,
                DrawerMenuItem(
                    R.drawable.ic_volume,
                    R.string.text_volume_down_control,
                    R.string.key_use_volume_control_record,
                    null
                ),

                DrawerMenuGroup(R.string.text_others),
                connectionItem,
                DrawerMenuItem(
                    R.drawable.ic_personalize,
                    R.string.text_theme_color,
                    this::openThemeColorSettings
                ),
                DrawerMenuItem(
                    R.drawable.ic_night_mode,
                    R.string.theme_setting
                ) { ThemeUtils.showCompat(it.itemView.context) },
                checkForUpdatesItem
            )
        )
        binding.drawerMenu.adapter = drawerMenuAdapter
        binding.drawerMenu.layoutManager = LinearLayoutManager(requireContext())
    }

    fun enableOrDisableAccessibilityService(holder: DrawerMenuItemViewHolder) {
        lifecycleScope.launch {
            val checked = holder.getSwitchCompat().isChecked
            val tool = AccessibilityServiceUtils
            when {
                checked && tool.isEnabled().not() -> tool.enableServiceCompat()
                checked.not() && tool.isEnabled() -> {
                    val isSuccess = AccessibilityService.disable()
                    if (isSuccess) return@launch
                    tool.openSetting(requireContext()) { enabled ->
                        setChecked(accessibilityServiceItem, enabled)
                    }
                }
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
        val checked = holder.getSwitchCompat().isChecked
        val enabled = PermissionManager.checkCompat(requireContext(), PACKAGE_USAGE_STATS)
        when {
            checked && enabled.not() -> {
                NotAskAgainDialog.Builder(requireContext(), "DrawerFragment.usage_stats")
                    .title(R.string.text_usage_stats_permission)
                    .content(R.string.description_usage_stats_permission)
                    .positiveText(R.string.ok)
                    .dismissListener { requestUsageStatsPermission() }
                    .show() ?: run { requestUsageStatsPermission() }
            }

            else -> {
                requestUsageStatsPermission()
            }
        }
    }

    private fun requestUsageStatsPermission() {
        PermissionManager.openSettings(requireContext(), PACKAGE_USAGE_STATS) { isGranted ->
            setChecked(usageStatsPermissionItem, isGranted)
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
                enableAccessibilityService()
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
            startForegroundService()
        } else {
            ForegroundService.stop(requireContext())
            setChecked(foregroundServiceItem, false)
        }
    }

    fun onShizukuToggle(holder: DrawerMenuItemViewHolder) {
        val checked = holder.getSwitchCompat().isChecked
        if (checked) {
            try {
                Shell.requestPermission(REQUEST_CODE_SHIZUKU)
            } catch (e: IllegalStateException) {
                setChecked(shizukuItem, false)
                GlobalAppContext.toast(e.message ?: "Unknown error")
            }
        } else {
            Shell.unbindService()
            setChecked(shizukuItem, false)
        }
    }

    private fun startForegroundService() {
        PermissionManager.requestNotification(requireContext(), ForegroundService.CHANNEL_ID) { granted ->
            if (granted) {
                ForegroundService.start(requireContext())
                setChecked(foregroundServiceItem, true)
            } else {
                setChecked(foregroundServiceItem, false)
            }
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
        Toast.makeText(requireContext(), getString(R.string.error_connect_to_remote, e.message), Toast.LENGTH_LONG)
            .show()
    }

    @Suppress("UNUSED_PARAMETER")
    fun checkForUpdates(_holder: DrawerMenuItemViewHolder) {
        Browser.openUrl(requireContext(), getString(R.string.my_github) + "/releases")
    }

    override fun onResume() {
        super.onResume()
        syncSwitchState()
    }

    private fun syncSwitchState() {
        setChecked(accessibilityServiceItem, checkService())
        setChecked(notificationPermissionItem, NotificationListenerService.instance != null)
        setChecked(usageStatsPermissionItem, PermissionManager.checkCompat(requireContext(), PACKAGE_USAGE_STATS))
        setChecked(shizukuItem, Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PERMISSION_GRANTED)
    }

    private fun enableAccessibilityService() {
        val shouldEnable = AccessibilityServiceUtils.byRoot
        val serviceRunning = checkService()
        if (shouldEnable.not() || serviceRunning) {
            return
        }
        setProgress(accessibilityServiceItem, true)
        lifecycleScope.launch {
            AccessibilityServiceUtils.enableServiceCompat()
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
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
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

    private fun checkService(): Boolean {
        return AccessibilityServiceUtils.isEnabled()
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        setChecked(shizukuItem, Shizuku.checkSelfPermission() == PERMISSION_GRANTED)
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        setChecked(shizukuItem, false)
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PERMISSION_GRANTED) {
            Shell.bindUserService()
            setChecked(shizukuItem, true)
        } else {
            setChecked(shizukuItem, false)
        }
    }

    companion object {
        private const val PERMISSION_GRANTED = 0
    }
}
