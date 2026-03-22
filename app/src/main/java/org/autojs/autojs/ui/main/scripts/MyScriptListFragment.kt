package org.autojs.autojs.ui.main.scripts

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.permission.PermissionManager
import com.stardust.util.IntentUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.databinding.FragmentMyScriptListBinding
import org.autojs.autojs.external.fileprovider.AppFileProvider
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerItem
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.Scripts
import org.autojs.autojs.tool.SimpleObserver
import org.autojs.autojs.ui.common.ScriptOperations
import org.autojs.autojs.ui.explorer.ExplorerView
import org.autojs.autojs.ui.main.FloatingActionMenu
import org.autojs.autojs.ui.main.QueryEvent
import org.autojs.autojs.ui.main.ViewPagerFragment
import org.autojs.autojs.ui.project.ProjectConfigActivity
import org.autojs.autojs.ui.viewmodel.ExplorerItemList
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class MyScriptListFragment : ViewPagerFragment, FloatingActionMenu.OnFloatingActionButtonClickListener {

    private var _binding: FragmentMyScriptListBinding? = null
    private val binding get() = _binding!!

    private var floatingActionMenu: FloatingActionMenu? = null

    constructor() : super(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): android.view.View {
        _binding = FragmentMyScriptListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()
        checkAndRequestPermissions()
    }

    private fun setUpViews() {
        val sortConfig = ExplorerItemList.SortConfig.from(PreferenceManager.getDefaultSharedPreferences(requireContext()))
        binding.scriptFileList.setSortConfig(sortConfig)
        binding.scriptFileList.setExplorer(Explorers.workspace(), ExplorerDirPage.createRoot(Pref.getScriptDirPath()))
        binding.scriptFileList.setOnItemClickListener(object : ExplorerView.OnItemClickListener {
            override fun onItemClick(view: android.view.View, item: ExplorerItem) {
                if (item.isEditable) {
                    Scripts.edit(requireContext(), item.toScriptFile())
                } else {
                    IntentUtil.viewFile(GlobalAppContext.get(), item.path, AppFileProvider.AUTHORITY)
                }
            }
        })
    }

    private fun checkAndRequestPermissions() {
        if (hasStoragePermission()) {
            refreshAll()
            return
        }
        showInfo()
    }

    private fun showInfo() {
        Snackbar.make(binding.root, R.string.text_no_storage_permission, Snackbar.LENGTH_SHORT)
            .setAction(R.string.text_request_permission) {
                PermissionManager.requestRuntime(
                    requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) { granted ->
                    if (granted) Explorers.workspace().refreshAll()
                }
            }.show()
    }

    private fun hasStoragePermission(): Boolean {
        return PermissionManager.checkCompat(
            requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun refreshAll() {
        Explorers.workspace().refreshAll()
    }

    override fun onFabClick(fab: FloatingActionButton) {
        initFloatingActionMenuIfNeeded(fab)
        if (floatingActionMenu?.isExpanded == true) {
            floatingActionMenu?.collapse()
        } else {
            floatingActionMenu?.expand()
        }
    }

    private fun initFloatingActionMenuIfNeeded(fab: FloatingActionButton) {
        if (floatingActionMenu != null) return
        floatingActionMenu = activity?.findViewById(org.autojs.autojs.R.id.floating_action_menu)
        floatingActionMenu?.state
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe(object : SimpleObserver<Boolean>() {
                override fun onNext(expanding: Boolean) {
                    fab.animate()
                        .rotation(if (expanding) 45f else 0f)
                        .setDuration(300)
                        .start()
                }
            })
        floatingActionMenu?.setOnFloatingActionButtonClickListener(this)
    }

    override fun onBackPressed(activity: Activity): Boolean {
        if (floatingActionMenu?.isExpanded == true) {
            floatingActionMenu?.collapse()
            return true
        }
        if (binding.scriptFileList.canGoBack()) {
            binding.scriptFileList.goBack()
            return true
        }
        return false
    }

    override fun onPageHide() {
        super.onPageHide()
        if (floatingActionMenu?.isExpanded == true) {
            floatingActionMenu?.collapse()
        }
    }

    @Subscribe
    fun onQuerySummit(event: QueryEvent) {
        if (isShown.not()) return
        if (event == QueryEvent.CLEAR) {
            binding.scriptFileList.setFilter { true }
            return
        }
        val query = event.query
        binding.scriptFileList.setFilter { item -> item.name.contains(query) }
    }

    override fun onStop() {
        super.onStop()
        binding.scriptFileList.getSortConfig().saveInto(PreferenceManager.getDefaultSharedPreferences(requireContext()))
    }

    override fun onDetach() {
        super.onDetach()
        floatingActionMenu?.setOnFloatingActionButtonClickListener(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onClick(button: FloatingActionButton, pos: Int) {
        if (_binding == null) return
        val currentPage = binding.scriptFileList.getCurrentPage()
        when (pos) {
            0 -> ScriptOperations(requireContext(), binding.scriptFileList, currentPage).newDirectory()
            1 -> ScriptOperations(requireContext(), binding.scriptFileList, currentPage).newFile()
            2 -> ScriptOperations(requireContext(), binding.scriptFileList, currentPage).importFile()
            3 -> startActivity(
                Intent(requireContext(), ProjectConfigActivity::class.java)
                    .putExtra(ProjectConfigActivity.EXTRA_PARENT_DIRECTORY, currentPage.path)
                    .putExtra(ProjectConfigActivity.EXTRA_NEW_PROJECT, true)
            )
        }
    }
}
