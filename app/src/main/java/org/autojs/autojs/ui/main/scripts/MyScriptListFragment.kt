package org.autojs.autojs.ui.main.scripts

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.stardust.autojs.permission.PermissionManager
import org.autojs.autojs.PrefV2
import org.autojs.autojs.R
import org.autojs.autojs.databinding.FragmentMyScriptListBinding
import org.autojs.autojs.external.fileprovider.AppFileProvider
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
import timber.log.Timber
import java.io.File

class MyScriptListFragment : BaseFragment(R.layout.fragment_my_script_list) {

    private var _binding: FragmentMyScriptListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        EventBus.getDefault().register(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyScriptListBinding.bind(view)
        setupScriptListView()
        setupBackPressHandler()
        checkPermission()
    }

    private fun setupScriptListView() {
        val sortConfig = ExplorerItemList.SortConfig.from(
            PrefV2.defaultPrefs
        )
        binding.scriptFileList.sortConfig = sortConfig
        binding.scriptFileList.setExplorer(
            Explorers.workspace(),
            ExplorerDirPage.createRoot(PrefV2.getScriptDirPath())
        )
        binding.scriptFileList.setOnItemClickListener { _, item ->
            if (item.isEditable()) {
                Scripts.edit(requireActivity(), item.toScriptFile())
            } else {
                openFile(item.path)
            }
        }
    }

    /*TODO: 浏览器(via,chrome)无法打开html，提示：requires the provider be exported, or grantUriPermission()*/
    private fun openFile(path: String) {
        if (path.isEmpty()) return
        val context = requireContext()
        val uri = AppFileProvider.getUriForFile(context, File(path))
        val mimeType = context.contentResolver.getType(uri) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.text_open_by_other_apps)))
        } catch (e: Exception) {
            Timber.e(e)
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

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.scriptFileList.canGoBack()) {
                        binding.scriptFileList.goBack()
                    } else {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
    }

    private fun checkPermission() {
        if (hasStoragePermission()) {
            Explorers.workspace().refreshAll()
            return
        }
        requireActivity()
        showSnackbar(
            message = "需要存储权限读写文件",
            actionText = "授权"
        ) {
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            requestPermission(
                READ_EXTERNAL_STORAGE
            ) { isGranted ->
                if (isGranted) Explorers.workspace().refreshAll()
            }
            return
        }
        PermissionManager.requestPermission(
            requireActivity(),
            MANAGE_EXTERNAL_STORAGE
        )
    }

    private fun hasStoragePermission(): Boolean {
        val permission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> MANAGE_EXTERNAL_STORAGE
            else -> READ_EXTERNAL_STORAGE
        }
        return hasPermission(permission)
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

//    NOTE: 默认路径变更，刷新当前页面
    @Subscribe
    fun onGlobalExplorerChange(event: ExplorerChangeEvent) {
        if (event.action == ExplorerChangeEvent.ALL) {
            binding.scriptFileList.setExplorer(
                Explorers.workspace(),
                ExplorerDirPage.createRoot(PrefV2.getScriptDirPath())
            )
        }
    }

    override fun onDestroyView() {
        binding.scriptFileList.sortConfig?.saveInto(
            PrefV2.defaultPrefs
        )
        _binding = null
        super.onDestroyView()
        Timber.d("%s onDestroyView", this::class.java.simpleName)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("%s onDestroy", this::class.java.simpleName)
        EventBus.getDefault().unregister(this)
    }
}