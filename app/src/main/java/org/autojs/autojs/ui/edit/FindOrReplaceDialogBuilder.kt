package org.autojs.autojs.ui.edit

import android.content.Context
import android.preference.PreferenceManager
import android.text.TextUtils
import android.widget.CheckBox
import com.afollestad.materialdialogs.MaterialDialog
import org.autojs.autojs.R
import org.autojs.autojs.databinding.DialogFindOrReplaceBinding
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.ui.edit.editor.CodeEditor

class FindOrReplaceDialogBuilder(
    context: Context,
    private val editorView: EditorView
) : ThemeColorMaterialDialogBuilder(context) {

    private val binding = DialogFindOrReplaceBinding.inflate(
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as android.view.LayoutInflater
    )

    private val regexCheckBox: CheckBox = binding.checkboxRegex
    private val replaceCheckBox: CheckBox = binding.checkboxReplace
    private val replaceAllCheckBox: CheckBox = binding.checkboxReplaceAll
    private val keywordsEditText = binding.keywords
    private val replacementEditText = binding.replacement

    init {
        setupViews()
        restoreState()
        setupListeners()
        autoDismiss(false)
        onNegative { dialog, _ -> dialog.dismiss() }
        onPositive { dialog, _ ->
            storeState()
            findOrReplace(dialog)
        }
    }

    private fun setupViews() {
        customView(binding.root, true)
        positiveText(R.string.ok)
        negativeText(R.string.cancel)
        title(R.string.text_find_or_replace)
    }

    private fun setupListeners() {
        replaceAllCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !replaceCheckBox.isChecked) {
                replaceCheckBox.isChecked = true
            }
        }

        replacementEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.isNotEmpty() == true) {
                    replaceCheckBox.isChecked = true
                }
            }
        })
    }

    private fun storeState() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(KEY_KEYWORDS, keywordsEditText.text.toString())
            .apply()
    }

    private fun restoreState() {
        keywordsEditText.setText(
            PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_KEYWORDS, "")
        )
    }

    private fun findOrReplace(dialog: MaterialDialog) {
        val keywords = keywordsEditText.text.toString()
        if (keywords.isEmpty()) return

        try {
            val usingRegex = regexCheckBox.isChecked
            if (!replaceCheckBox.isChecked) {
                editorView.find(keywords, usingRegex)
            } else {
                val replacement = replacementEditText.text.toString()
                if (replaceAllCheckBox.isChecked) {
                    editorView.replaceAll(keywords, replacement, usingRegex)
                } else {
                    editorView.replace(keywords, replacement, usingRegex)
                }
            }
            dialog.dismiss()
        } catch (_: CodeEditor.CheckedPatternSyntaxException) {
            keywordsEditText.error = context.getString(R.string.error_pattern_syntax)
        }
    }

    fun setQueryIfNotEmpty(s: String?): FindOrReplaceDialogBuilder {
        if (!TextUtils.isEmpty(s)) {
            keywordsEditText.setText(s)
        }
        return this
    }

    companion object {
        private const val KEY_KEYWORDS = "..."
    }
}
