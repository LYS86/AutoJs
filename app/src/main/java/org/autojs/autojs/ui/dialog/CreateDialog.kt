package org.autojs.autojs.ui.dialog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.autojs.autojs.R
import org.autojs.autojs.databinding.DialogImportBinding
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.ui.common.ScriptOperationsV2
import java.io.File

class CreateDialog : DialogFragment() {

    private var _binding: DialogImportBinding? = null
    private val binding get() = _binding!!

    private lateinit var explorerPage: ExplorerDirPage

    companion object {
        private const val ARG_EXPLORER_PAGE = "explorer_page"

        fun newInstance(explorerPage: ExplorerDirPage): CreateDialog {
            return CreateDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_EXPLORER_PAGE, explorerPage.path)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        _binding = DialogImportBinding.inflate(LayoutInflater.from(requireContext()))

        val pagePath = requireArguments().getString(ARG_EXPLORER_PAGE)
        explorerPage = ExplorerDirPage.createRoot(pagePath!!)

        setupInputValidation()

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.DialogTheme)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .setNeutralButton(R.string.text_import) { _, _ -> handleImportFile() }
            .create()
            .apply { setCanceledOnTouchOutside(false) }

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.isEnabled = false
            positiveButton.setOnClickListener { handleCreateAction() }
        }

        return dialog
    }

    private fun setupInputValidation() {
        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInput(s?.toString() ?: "")
            }
        })
    }

    private fun validateInput(input: String) {
        val dialog = dialog as? AlertDialog
        val okButton = dialog?.getButton(AlertDialog.BUTTON_POSITIVE)

        if (input.isBlank()) {
            clearError()
            okButton?.isEnabled = false
            return
        }

        val finalName = when {
            binding.radioFile.isChecked -> input.takeIf { "." in it } ?: "$input.js"
            else -> input
        }
        val targetFile = File(explorerPage.path, finalName)
        val exists = targetFile.exists()

        if (exists) {
            setError(getString(R.string.text_file_exists))
            okButton?.isEnabled = false
        } else {
            clearError()
            okButton?.isEnabled = true
        }
    }

    private fun clearError() {
        binding.textInputLayout.run {
            error = null
            isErrorEnabled = false
        }
    }

    private fun setError(text: String) {
        binding.textInputLayout.run {
            isErrorEnabled = true
            error = text
        }
    }

    private fun handleCreateAction() {
        val inputName = binding.editText.text.toString().trim()
        if (inputName.isBlank()) return

        val currentDirectory = ScriptFile(explorerPage.path)
        val operations = ScriptOperationsV2(requireContext(), view, currentDirectory)

        lifecycleScope.launch {
            when {
                binding.radioFile.isChecked -> {
                    val fileName = inputName.takeIf { "." in it } ?: "$inputName.js"
                    operations.createFile(fileName, editor = true)
                }

                binding.radioFolder.isChecked -> operations.createFolder(inputName)
                binding.radioProject.isChecked -> operations.createProject(inputName, true)
            }
        }
        dismiss()
    }

    private fun handleImportFile() {
        val currentDirectory = ScriptFile(explorerPage.path)
        val operations =
            org.autojs.autojs.ui.common.ScriptOperations(requireContext(), view, currentDirectory)
        operations.importFile()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}