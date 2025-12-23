package org.autojs.autojs.ui.main.scripts

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import com.stardust.autojs.permission.PermissionManager
import org.autojs.autojs.PrefV2
import org.autojs.autojs.R
import org.autojs.autojs.databinding.FragmentMyScriptListBinding
import org.autojs.autojs.external.fileprovider.AppFileProvider
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.Scripts
import org.autojs.autojs.ui.base.BaseFragment
import org.autojs.autojs.ui.dialog.CreateDialog
import org.autojs.autojs.ui.main.QueryEvent
import org.autojs.autojs.ui.viewmodel.ExplorerItemList.SortConfig
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import timber.log.Timber
import java.io.File

class MyScriptListFragment : BaseFragment(R.layout.fragment_my_script_list) {

    private var _binding: FragmentMyScriptListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyScriptListBinding.bind(view)
        setupScriptListView()
        setupBackPressHandler()
        setupFab()
        checkPermission()
    }

    private fun setupScriptListView() {
        val sortConfig = SortConfig.from(
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

    private fun setupFab() {
        binding.fab.setOnClickListener {
            showImportDialog()
        }
    }

    private fun showImportDialog() {
        val currentPage = binding.scriptFileList.currentPage
        if (currentPage is ExplorerDirPage) {
            val createDialog = CreateDialog.newInstance(currentPage)
            createDialog.show(childFragmentManager, "CreateDialog")
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.scriptFileList.canGoBack()) {
                        binding.scriptFileList.goBack()
                        return
                    } else {
                        isEnabled = false
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
            message = getString(R.string.text_need_storage_permission),
            actionText = getString(R.string.text_grant_permission)
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

    override fun onDestroyView() {
        binding.scriptFileList.sortConfig?.saveInto(
            PrefV2.defaultPrefs
        )
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}