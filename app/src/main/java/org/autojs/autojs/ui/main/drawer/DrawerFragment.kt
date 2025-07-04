package org.autojs.autojs.ui.main.drawer

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shizuku.Utils
import com.stardust.app.GlobalAppContext
import com.stardust.app.hasPermission
import com.stardust.app.isOpPermissionGranted
import com.stardust.notification.NotificationListenerService
import com.stardust.util.IntentUtil2
import kotlinx.coroutines.launch
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.databinding.FragmentDrawerBinding
import org.autojs.autojs.external.foreground.ForegroundService
import org.autojs.autojs.pluginclient.DevPluginService2
import org.autojs.autojs.tool.AccessibilityServiceTool3
import org.autojs.autojs.tool.PermissionTool
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.common.DialogUtils
import org.autojs.autojs.ui.common.MessageUtils
import org.autojs.autojs.ui.floating.CircularMenu
import org.autojs.autojs.ui.floating.FloatyWindowManager
import org.autojs.autojs.ui.main.MainActivity
import org.autojs.autojs.ui.settings.SettingsActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import kotlin.system.exitProcess

class DrawerFragment : Fragment() {
    private val lifecycleScope: LifecycleCoroutineScope
        get() = lifecycle.coroutineScope
    private var _binding: FragmentDrawerBinding? = null
    private val binding get() = _binding!!
    private lateinit var mDrawerMenuAdapter: DrawerMenuAdapter
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val mStableModeItem = DrawerMenuItem(
        R.drawable.ic_stable,
        R.string.text_stable_mode,
        R.string.key_stable_mode,
    ) { holder ->
        holder.switchCompat.isChecked.also {
            if (it) {
                showStableModePromptIfNeeded()
            }
        }
    }

    private val mNotificationPermissionItem = DrawerMenuItem(
        R.drawable.ic_ali_notification, R.string.text_notification_permission, 0
    ) { holder ->
        goToNotificationServiceSettings(holder)
    }

    private val mForegroundServiceItem = DrawerMenuItem(
        R.drawable.ic_service_green, R.string.text_foreground_service, 0
    ) { holder ->
        toggleForegroundService(holder)
    }

    private val mUsageStatsPermissionItem = DrawerMenuItem(
        R.drawable.ic_ali_notification, R.string.text_usage_stats_permission, 0
    ) { holder ->
        goToUsageStatsSettings(holder)
    }

    private val mFloatingWindowItem =
        DrawerMenuItem(R.drawable.ic_robot_64, R.string.text_floating_window, 0) { holder ->
            showOrDismissFloatingWindow(holder)
        }

    private val mCheckForUpdatesItem =
        DrawerMenuItem(R.drawable.ic_check_for_updates, R.string.text_check_for_updates) { holder ->
            checkForUpdates(holder)
        }

    private val mConnectionItem =
        DrawerMenuItem(R.drawable.ic_connect_to_pc, R.string.debug, 0) { holder ->
            connectOrDisconnectToRemote(holder)
        }

    private val mAccessibilityServiceItem = DrawerMenuItem(
        R.drawable.ic_service_green, R.string.text_accessibility_service, 0
    ) { holder ->
        switchAccessibilityService(holder)
    }

    private val mShizukuItem =
        DrawerMenuItem(R.drawable.ic_service_green, R.string.text_shizuku_permission) { holder ->
            requestShizukuPermission()
        }


    private fun inputRemoteHost() {
        val host = DevPluginService2.getInstance().serverAddress
        DialogUtils.custom(requireActivity())
            .title(R.string.text_server_address)
            .input("", host) { _, input ->
                DevPluginService2.getInstance().connectToServer(input.toString())
            }.cancelListener {
                setChecked(mConnectionItem, false)
            }.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMenuItems()
        initButton()
        setupStates()
    }

    /**
     * 恢复状态
     * @see DrawerMenuItem
     */
    private fun setupStates() {
        setChecked(mFloatingWindowItem, FloatyWindowManager.restore())
        if (Pref.isForegroundServiceEnabled()) {
            ForegroundService.start(GlobalAppContext.get())
            setChecked(mForegroundServiceItem, true)
        }
        lifecycleScope.launch {
            DevPluginService2.getInstance().connectionState.collect { state ->
                mConnectionItem.let {
                    val isConnected = state is DevPluginService2.State.Connected
                    val inProgress = state is DevPluginService2.State.Connecting
                    it.let {
                        if (isConnected != it.isChecked) {
                            setChecked(it, isConnected)
                        }
                        if (inProgress != it.isProgress) {
                            setProgress(it, inProgress)
                        }
                    }
                }
                if (state is DevPluginService2.State.Error) {
                    showMessage(state.exception.message ?: "")
                }
            }
        }
        DevPluginService2.getInstance().restore()
    }

    private fun initButton() {
        binding.drawerMenu.adapter = mDrawerMenuAdapter
        binding.drawerMenu.layoutManager = LinearLayoutManager(context)
        binding.setting.setOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
        }
        binding.exit.setOnClickListener {
            activity?.finishAffinity()
            exitProcess(0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }


    private fun checkForUpdates(holder: DrawerMenuItemViewHolder) {
        setProgress(mCheckForUpdatesItem, true)
        showMessage("功能维护中")
        setProgress(mCheckForUpdatesItem, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    ForegroundService.start(requireContext())
                    setChecked(mForegroundServiceItem, true)
                } else {
                    showMessage(R.string.foreground_service_need_notification_permission)
                    setChecked(mForegroundServiceItem, false)
                }
            }

        EventBus.getDefault().register(this)
    }

    private fun initMenuItems() {
        mDrawerMenuAdapter = DrawerMenuAdapter(
            arrayListOf(
                DrawerMenuGroup(R.string.text_service),
                mAccessibilityServiceItem,
                mStableModeItem,
                mNotificationPermissionItem,
                mForegroundServiceItem,
                mUsageStatsPermissionItem,
                mShizukuItem,
                DrawerMenuGroup(R.string.text_script_record),
                mFloatingWindowItem,
                DrawerMenuItem(
                    R.drawable.ic_volume,
                    R.string.text_volume_down_control,
                    R.string.key_use_volume_control_record,
                    null
                ),
                DrawerMenuGroup(R.string.text_others),
                mConnectionItem,
                DrawerMenuItem(
                    R.drawable.ic_personalize, R.string.text_theme_color
                ) { holder -> openThemeColorSettings() },
                DrawerMenuItem(
                    R.drawable.ic_night_mode, R.string.text_night_mode, R.string.key_night_mode
                ) { holder -> toggleNightMode(holder) },
                mCheckForUpdatesItem
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private fun goToNotificationServiceSettings(holder: DrawerMenuItemViewHolder) {
        val enabled = NotificationListenerService.hasNotificationAccess(requireContext())
        val checked = holder.switchCompat.isChecked
        if (enabled == checked) return
        Timber.d("通知权限状态: $enabled, 开关状态: $checked")
        PermissionTool().apply {
            add(task = {
                NotificationListenerService.toSettings(requireContext()).also {
                    Timber.d("跳转通知设置")
                }
            }, onError = { error ->
                Timber.e(error, "跳转通知设置失败")
                setChecked(mNotificationPermissionItem, !checked)
            })

            check(task = {
                val hasPermission =
                    NotificationListenerService.hasNotificationAccess(requireContext())
                Timber.d("检查通知权限: 当前状态=$hasPermission, 目标状态=$checked")
                hasPermission == checked
            }, timeout = 30_000L, onSuccess = {
                Timber.d("通知权限状态切换成功")
                startActivity(Intent(requireContext(), MainActivity::class.java))
            }, onError = { error ->
                Timber.e(error, "检查通知权限失败")
                setChecked(mNotificationPermissionItem, !checked)
            })

            start()
        }
    }

    private fun goToUsageStatsSettings(holder: DrawerMenuItemViewHolder) {
        val enabled = requireContext().hasPermission(AppOpsManager.OPSTR_GET_USAGE_STATS)
        val checked = holder.switchCompat.isChecked
        if (enabled == checked) return
        Timber.d("使用情况访问权限状态: $enabled, 开关状态: $checked")
        PermissionTool().apply {
            add(task = {
                IntentUtil2.requestAppUsagePermission(requireContext()).also {
                    Timber.d("跳转使用情况访问权限设置")
                }
            }, onError = { error ->
                Timber.e(error, "跳转使用情况访问权限设置失败")
                setChecked(mUsageStatsPermissionItem, !checked)
            })

            check(task = {
                val hasProgression =
                    requireContext().isOpPermissionGranted(AppOpsManager.OPSTR_GET_USAGE_STATS)
                Timber.d("检查使用情况访问权限: 当前状态=$hasProgression, 目标状态=$checked")
                hasProgression == checked
            }, timeout = 30_000L, onSuccess = {
                Timber.d("使用情况访问权限状态切换成功")
                startActivity(Intent(requireContext(), MainActivity::class.java))
            }, onError = { error ->
                Timber.e(error, "检查使用情况访问权限失败")
                setChecked(mUsageStatsPermissionItem, !checked)
            })

            start()
        }
    }

    private fun switchAccessibilityService(holder: DrawerMenuItemViewHolder) {
        val checked = holder.switchCompat.isChecked
        lifecycleScope.launch {
            AccessibilityServiceTool3.switchServiceSuspend(checked).also {
                setChecked(mAccessibilityServiceItem, it)
                if (!it == checked) AccessibilityServiceTool3.toSetting()
            }
        }
    }

    fun showOrDismissFloatingWindow(holder: DrawerMenuItemViewHolder) {
        val isChecked = holder.switchCompat.isChecked
        val activity = requireActivity()

        if (!isChecked) {
            FloatyWindowManager.switchMenu(false)
            return
        }

        FloatyWindowManager.requestPermission(activity = activity, onSuccess = {
            FloatyWindowManager.switchMenu()
            startActivity(Intent(requireContext(), MainActivity::class.java))
        }, onError = {
            setChecked(mFloatingWindowItem, false)
            Timber.e(it, "请求悬浮窗权限失败")
        })

    }

    private fun openThemeColorSettings() {
        SettingsActivity.selectThemeColor(activity)
    }

    private fun toggleNightMode(holder: DrawerMenuItemViewHolder) {
        (activity as? BaseActivity)?.setNightModeEnabled(holder.switchCompat.isChecked)
    }

    private fun connectOrDisconnectToRemote(holder: DrawerMenuItemViewHolder) {
        val checked = holder.switchCompat.isChecked
        val connected = DevPluginService2.getInstance().isConnected
        if (checked && !connected) {
            inputRemoteHost()
        } else if (!checked && connected) {
            DevPluginService2.getInstance().disconnect()
        }
    }

    private fun toggleForegroundService(holder: DrawerMenuItemViewHolder) {
        val checked = holder.switchCompat.isChecked
        if (!checked) {
            ForegroundService.stop(requireContext())
            return
        }

        val hasPermission = ForegroundService.hasNotificationPermission(requireContext())
        if (!hasPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                showMessage(R.string.foreground_service_need_notification_permission)
            }
            setChecked(mForegroundServiceItem, false)
            return
        }
        ForegroundService.start(requireContext())
    }

    private fun showStableModePromptIfNeeded() {
        DialogUtils.showBasic(
            context = requireContext(),
            title = getString(R.string.text_stable_mode),
            content = getString(R.string.description_stable_mode),
            positiveText = getString(R.string.ok)
        )
    }

    override fun onResume() {
        super.onResume()
        syncSwitchState()
    }

    /**
     * 同步开关状态
     */
    private fun syncSwitchState() {
        setChecked(
            mAccessibilityServiceItem, AccessibilityServiceTool3.isEnabled(requireContext())
        )
        setChecked(mNotificationPermissionItem, NotificationListenerService.instance != null)
        setChecked(
            mUsageStatsPermissionItem,
            context?.isOpPermissionGranted(AppOpsManager.OPSTR_GET_USAGE_STATS) == true
        )


    }

    @Subscribe
    fun onCircularMenuStateChange(event: CircularMenu.StateChangeEvent) {
        setChecked(mFloatingWindowItem, event.currentState != CircularMenu.STATE_CLOSED)
    }

    private fun showMessage(id: Int, forceToast: Boolean = false) {
        showMessage(getString(id), forceToast = forceToast)
    }

    private fun showMessage(text: CharSequence, forceToast: Boolean = false) {
        MessageUtils.show(
            context = requireContext(),
            view = view,
            message = text.toString(),
            forceToast = forceToast
        )
    }

    private fun setProgress(item: DrawerMenuItem, progress: Boolean) {
        item.isProgress = progress
        mDrawerMenuAdapter.notifyItemChanged(item)
    }

    private fun setChecked(item: DrawerMenuItem, checked: Boolean) {
        item.isChecked = checked
        mDrawerMenuAdapter.notifyItemChanged(item)
    }

    private fun requestShizukuPermission() {
        when {
            !Utils.hasApp() -> {
                showMessage(R.string.text_shizuku_app_not_installed)
            }

            !Utils.isReady() -> {
                DialogUtils.showConfirm(
                    context = requireContext(),
                    title = getString(R.string.text_shizuku_service_not_ready),
                    content = getString(R.string.text_to_shizuku),
                    onPositive = { Utils.launchApp() })
            }

            else -> {
                lifecycleScope.launch {
                    val granted = Utils.requestPermissionSuspend()
                    if (granted) {
                        showMessage(R.string.text_shizuku_hasPermission)
                    } else {
                        showMessage(R.string.text_shizuku_permission_denied)
                    }
                }
            }
        }
    }
}
