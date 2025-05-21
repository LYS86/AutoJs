package org.autojs.autojs.ui.main.scripts

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.databinding.FragmentMyScriptListBinding
import org.autojs.autojs.model.explorer.ExplorerChangeEvent
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.Scripts
import org.autojs.autojs.ui.base.BaseFragment
import org.autojs.autojs.ui.common.ScriptOperations
import org.autojs.autojs.ui.main.QueryEvent
import org.autojs.autojs.ui.project.ProjectConfigActivity
import org.autojs.autojs.ui.viewmodel.ExplorerItemList
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class MyScriptListFragment : BaseFragment() {

    private var _binding: FragmentMyScriptListBinding? = null
    private val binding get() = _binding!!

    private val requestManageStorage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasStoragePermission()) {
            Explorers.workspace().refreshAll()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        checkPermissions()
        EventBus.getDefault().register(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyScriptListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupScriptListView()
    }

    private fun setupScriptListView() {
        val sortConfig = ExplorerItemList.SortConfig.from(
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        binding.scriptFileList.sortConfig = sortConfig
        binding.scriptFileList.setExplorer(
            Explorers.workspace(),
            ExplorerDirPage.createRoot(Pref.getScriptDirPath())
        )
        binding.scriptFileList.setOnItemClickListener { v, item ->
            if (item.isEditable()) {
                Scripts.edit(requireActivity(), item.toScriptFile())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_script_list, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_new_directory -> {
                ScriptOperations(
                    requireContext(), binding.scriptFileList,
                    binding.scriptFileList.currentPage
                ).newDirectory()
                true
            }

            R.id.menu_new_file -> {
                ScriptOperations(
                    requireContext(), binding.scriptFileList,
                    binding.scriptFileList.currentPage
                ).newFile()
                true
            }

            R.id.menu_import -> {
                ScriptOperations(
                    requireContext(), binding.scriptFileList,
                    binding.scriptFileList.currentPage
                ).importFile()
                true
            }

            R.id.menu_new_project -> {
                startActivity(Intent(context, ProjectConfigActivity::class.java).apply {
                    putExtra(
                        ProjectConfigActivity.EXTRA_PARENT_DIRECTORY,
                        binding.scriptFileList.currentPage.path
                    )
                    putExtra(ProjectConfigActivity.EXTRA_NEW_PROJECT, true)
                })
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkPermissions() {
        if (!hasStoragePermission()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                requestPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) { isGranted ->
                    if (isGranted) {
                        Explorers.workspace().refreshAll()
                    }
                }
            } else {
                requestManageStorage.launch(
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                )
            }
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    @Subscribe
    fun onQuerySubmit(event: QueryEvent) {
        if (!isAdded) return
        if (event == QueryEvent.CLEAR) {
            binding.scriptFileList.setFilter(null)
            return
        }
        event.query?.let { query ->
            binding.scriptFileList.setFilter { item -> item.name.contains(query) }
        }
    }

    @Subscribe
    fun onGlobalExplorerChange(event: ExplorerChangeEvent) {
        if (event.action == ExplorerChangeEvent.ALL) {
            binding.scriptFileList.setExplorer(
                Explorers.workspace(),
                ExplorerDirPage.createRoot(Pref.getScriptDirPath())
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.scriptFileList.sortConfig?.saveInto(
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}