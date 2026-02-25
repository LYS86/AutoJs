package org.autojs.autojs.ui.floating

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.makeramen.roundedimageview.RoundedImageView
import com.stardust.app.DialogUtils
import org.autojs.autojs.databinding.CircularActionMenuBinding
import org.autojs.autojs.ui.common.OperationDialogBuilder
import com.stardust.autojs.core.record.Recorder
import com.stardust.enhancedfloaty.FloatyService
import com.stardust.enhancedfloaty.FloatyWindow
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.autojs.record.GlobalActionRecorder
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerItem
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.Scripts
import org.autojs.autojs.tool.AccessibilityServiceTool
import org.autojs.autojs.tool.RootTool
import org.autojs.autojs.ui.common.NotAskAgainDialog
import org.autojs.autojs.ui.floating.layoutinspector.LayoutBoundsFloatyWindow
import org.autojs.autojs.ui.floating.layoutinspector.LayoutHierarchyFloatyWindow
import org.autojs.autojs.ui.main.MainActivity_
import org.autojs.autojs.ui.explorer.ExplorerView
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import com.stardust.util.ClipboardUtil
import com.stardust.util.Func1
import com.stardust.view.accessibility.AccessibilityService
import com.stardust.view.accessibility.LayoutInspector
import com.stardust.view.accessibility.NodeInfo
import org.greenrobot.eventbus.EventBus
import org.jdeferred.Deferred
import org.jdeferred.impl.DeferredObject

/**
 * Created by Stardust on 2017/10/18.
 */

class CircularMenu(context: Context) : Recorder.OnStateChangedListener, LayoutInspector.CaptureAvailableListener,
    OperationDialogBuilder.OnItemClickListener {

    class StateChangeEvent(val currentState: Int, val previousState: Int)

    companion object {
        const val STATE_CLOSED = -1
        const val STATE_NORMAL = 0
        const val STATE_RECORDING = 1

        private val IC_ACTION_VIEW = R.drawable.ic_android_eat_js
    }

    private val mWindow: CircularMenuWindow
    private var mState: Int
    private var mActionViewIcon: RoundedImageView? = null
    private val mContext: Context = ContextThemeWrapper(context, R.style.AppTheme)
    private val mRecorder: GlobalActionRecorder
    private var mSettingsDialog: MaterialDialog? = null
    private var mLayoutInspectDialog: MaterialDialog? = null
    private var mRunningPackage: String? = null
    private var mRunningActivity: String? = null
    private var mCaptureDeferred: Deferred<NodeInfo, Void, Void>? = null

    init {
        mState = STATE_NORMAL
        mRecorder = GlobalActionRecorder.getSingleton(context)
        mRecorder.addOnStateChangedListener(this)
        AutoJs.getInstance().layoutInspector.addCaptureAvailableListener(this)
        mWindow = initFloaty()
        setupListeners()
    }

    private fun setupListeners() {
        mWindow.setOnActionViewClickListener { _ ->
            when {
                mState == STATE_RECORDING -> stopRecord()
                mWindow.isExpanded -> mWindow.collapse()
                else -> {
                    mCaptureDeferred = DeferredObject()
                    AutoJs.getInstance().layoutInspector.captureCurrentWindow()
                    mWindow.expand()
                }
            }
        }
    }

    private fun initFloaty(): CircularMenuWindow {
        val window = CircularMenuWindow(mContext, object : CircularMenuFloaty {

            override fun inflateActionView(service: FloatyService, window: CircularMenuWindow): View {
                val actionView = View.inflate(service, R.layout.circular_action_view, null)
                mActionViewIcon = actionView.findViewById(R.id.icon)
                return actionView
            }

            override fun inflateMenuItems(service: FloatyService, window: CircularMenuWindow): CircularActionMenu {
                val binding = CircularActionMenuBinding.inflate(LayoutInflater.from(ContextThemeWrapper(service, R.style.AppTheme)))
                setupMenuListeners(binding)
                return binding.root
            }
        })
        window.setKeepToSideHiddenWidthRadio(0.25f)
        FloatyService.addWindow(window)
        return window
    }

    private fun setupMenuListeners(binding: CircularActionMenuBinding) {
        binding.scriptList.setOnClickListener {
            showScriptList()
        }
        binding.record.setOnClickListener {
            startRecord()
        }
        binding.layoutInspect.setOnClickListener {
            inspectLayout()
        }
        binding.stopAllScripts.setOnClickListener {
            stopAllScripts()
        }
        binding.settings.setOnClickListener {
            settings()
        }
    }

    private fun showScriptList() {
        mWindow.collapse()
        val explorerView = ExplorerView(mContext)
        explorerView.setExplorer(Explorers.workspace(), ExplorerDirPage.createRoot(Pref.getScriptDirPath()))
        explorerView.setDirectorySpanSize(2)
        val dialog = ThemeColorMaterialDialogBuilder(mContext)
            .title(R.string.text_run_script)
            .customView(explorerView, false)
            .positiveText(R.string.cancel)
            .build()
        explorerView.setOnItemOperatedListener(object : ExplorerView.OnItemOperatedListener {
            override fun onItemOperated(item: ExplorerItem) {
                dialog.dismiss()
            }
        })
        explorerView.setOnItemClickListener(object : ExplorerView.OnItemClickListener {
            override fun onItemClick(view: View, item: ExplorerItem) {
                Scripts.run(item.toScriptFile())
            }
        })
        DialogUtils.showDialog(dialog)
    }

    private fun startRecord() {
        mWindow.collapse()
        if (!RootTool.isRootAvailable()) {
            DialogUtils.showDialog(NotAskAgainDialog.Builder(mContext, "CircularMenu.root")
                .title(R.string.text_device_not_rooted)
                .content(R.string.prompt_device_not_rooted)
                .neutralText(R.string.text_device_rooted)
                .positiveText(R.string.ok)
                .onNeutral { _, _ -> mRecorder.start() }
                .build())
        } else {
            mRecorder.start()
        }
    }

    private fun setState(state: Int) {
        val previousState = mState
        mState = state
        mActionViewIcon?.setImageResource(if (mState == STATE_RECORDING) R.drawable.ic_ali_record else IC_ACTION_VIEW)
        mActionViewIcon?.setBackgroundResource(if (mState == STATE_RECORDING) R.drawable.circle_red else R.drawable.circle_white)
        val padding = mContext.resources.getDimension(if (mState == STATE_RECORDING) R.dimen.padding_circular_menu_recording else R.dimen.padding_circular_menu_normal).toInt()
        mActionViewIcon?.setPadding(padding, padding, padding, padding)
        EventBus.getDefault().post(StateChangeEvent(mState, previousState))
    }

    private fun stopRecord() {
        mRecorder.stop()
    }

    private fun inspectLayout() {
        mWindow.collapse()
        mLayoutInspectDialog = OperationDialogBuilder(mContext)
            .item(R.id.layout_bounds, R.drawable.ic_circular_menu_bounds, R.string.text_inspect_layout_bounds)
            .item(R.id.layout_hierarchy, R.drawable.ic_circular_menu_hierarchy, R.string.text_inspect_layout_hierarchy)
            .bindItemClick(this)
            .title(R.string.text_inspect_layout)
            .build()
        DialogUtils.showDialog(mLayoutInspectDialog!!)
    }

    private fun showLayoutBounds() {
        inspectLayout { LayoutBoundsFloatyWindow(it) }
    }

    private fun showLayoutHierarchy() {
        inspectLayout { LayoutHierarchyFloatyWindow(it) }
    }

    private fun inspectLayout(windowCreator: Func1<NodeInfo, FloatyWindow>) {
        mLayoutInspectDialog?.dismiss()
        mLayoutInspectDialog = null
        if (AccessibilityService.instance == null) {
            Toast.makeText(mContext, R.string.text_no_accessibility_permission_to_capture, Toast.LENGTH_SHORT).show()
            AccessibilityServiceTool.goToAccessibilitySetting()
            return
        }
        val progress = DialogUtils.showDialog(ThemeColorMaterialDialogBuilder(mContext)
            .content(R.string.text_layout_inspector_is_dumping)
            .canceledOnTouchOutside(false)
            .progress(true, 0)
            .build())
        mCaptureDeferred?.promise()
            ?.then({
                mActionViewIcon?.post {
                    if (!progress.isCancelled) {
                        progress.dismiss()
                        FloatyService.addWindow(windowCreator.call(it))
                    }
                }
            }, {
                mActionViewIcon?.post { progress.dismiss() }
            })
    }

    private fun stopAllScripts() {
        mWindow.collapse()
        AutoJs.getInstance().scriptEngineService.stopAllAndToast()
    }

    override fun onCaptureAvailable(capture: NodeInfo?) {
        if (mCaptureDeferred?.isPending == true && capture != null) {
            mCaptureDeferred?.resolve(capture)
        }
    }

    override fun onItemClick(view: View, id: Int) {
        when (id) {
            R.id.layout_bounds -> showLayoutBounds()
            R.id.layout_hierarchy -> showLayoutHierarchy()
            R.id.accessibility_service -> enableAccessibilityService()
            R.id.package_name -> copyPackageName()
            R.id.class_name -> copyActivityName()
            R.id.open_launcher -> openLauncher()
            R.id.pointer_location -> togglePointerLocation()
            R.id.exit -> close()
        }
    }

    private fun settings() {
        mWindow.collapse()
        mRunningPackage = AutoJs.getInstance().infoProvider.getLatestPackageByUsageStatsIfGranted()
        mRunningActivity = AutoJs.getInstance().infoProvider.latestActivity
        mSettingsDialog = OperationDialogBuilder(mContext)
            .item(R.id.accessibility_service, R.drawable.ic_service_green, R.string.text_accessibility_settings)
            .item(R.id.package_name, R.drawable.ic_ali_app, mContext.getString(R.string.text_current_package) + mRunningPackage)
            .item(R.id.class_name, R.drawable.ic_ali_android, mContext.getString(R.string.text_current_activity) + mRunningActivity)
            .item(R.id.open_launcher, R.drawable.ic_android_eat_js, R.string.text_open_main_activity)
            .item(R.id.pointer_location, R.drawable.ic_zoom_out_map_white_24dp, R.string.text_pointer_location)
            .item(R.id.exit, R.drawable.ic_close_white_48dp, R.string.text_exit_floating_window)
            .bindItemClick(this)
            .title(R.string.text_more)
            .build()
        DialogUtils.showDialog(mSettingsDialog!!)
    }

    private fun enableAccessibilityService() {
        dismissSettingsDialog()
        AccessibilityServiceTool.enableAccessibilityService()
    }

    private fun dismissSettingsDialog() {
        mSettingsDialog?.dismiss()
        mSettingsDialog = null
    }

    private fun copyPackageName() {
        dismissSettingsDialog()
        if (TextUtils.isEmpty(mRunningPackage)) return
        ClipboardUtil.setClip(mContext, mRunningPackage!!)
        Toast.makeText(mContext, R.string.text_already_copy_to_clip, Toast.LENGTH_SHORT).show()
    }

    private fun copyActivityName() {
        dismissSettingsDialog()
        if (TextUtils.isEmpty(mRunningActivity)) return
        ClipboardUtil.setClip(mContext, mRunningActivity!!)
        Toast.makeText(mContext, R.string.text_already_copy_to_clip, Toast.LENGTH_SHORT).show()
    }

    private fun openLauncher() {
        dismissSettingsDialog()
        mContext.startActivity(Intent(mContext, MainActivity_::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun togglePointerLocation() {
        dismissSettingsDialog()
        RootTool.togglePointerLocation()
    }

    fun close() {
        dismissSettingsDialog()
        try {
            mWindow.close()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } finally {
            EventBus.getDefault().post(StateChangeEvent(STATE_CLOSED, mState))
            mState = STATE_CLOSED
        }
        mRecorder.removeOnStateChangedListener(this)
        AutoJs.getInstance().layoutInspector.removeCaptureAvailableListener(this)
    }

    override fun onStart() {
        setState(STATE_RECORDING)
    }

    override fun onStop() {
        setState(STATE_NORMAL)
    }

    override fun onPause() {
    }

    override fun onResume() {
    }
}
