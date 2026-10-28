package org.autojs.autojs.ui.edit;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.stardust.autojs.core.pref.DefaultSharedPreferences;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.DialogFindOrReplaceBinding;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autojs.ui.edit.editor.CodeEditor;
import org.autojs.autojs.ui.widget.SimpleTextWatcher;

/**
 * Created by Stardust on 2017/9/28.
 */

public class FindOrReplaceDialogBuilder extends ThemeColorMaterialDialogBuilder {

    private static final String KEY_KEYWORDS = "...";

    CheckBox mRegexCheckBox;

    CheckBox mReplaceCheckBox;

    CheckBox mReplaceAllCheckBox;

    EditText mKeywordsEditText;

    EditText mReplacementEditText;

    private EditorView mEditorView;

    public FindOrReplaceDialogBuilder(@NonNull Context context, EditorView editorView) {
        super(context);
        mEditorView = editorView;
        setupViews();
        restoreState();
        autoDismiss(false);
        onNegative((dialog, which) -> dialog.dismiss());
        onPositive((dialog, which) -> {
            storeState();
            findOrReplace(dialog);
        });
    }

    private void setupViews() {
        DialogFindOrReplaceBinding binding = DialogFindOrReplaceBinding.inflate(LayoutInflater.from(context));
        View view = binding.getRoot();
        mRegexCheckBox = binding.checkboxRegex;
        mReplaceCheckBox = binding.checkboxReplace;
        mReplaceAllCheckBox = binding.checkboxReplaceAll;
        mKeywordsEditText = binding.keywords;
        mReplacementEditText = binding.replacement;
        binding.checkboxReplaceAll.setOnCheckedChangeListener((buttonView, isChecked) -> syncWithReplaceCheckBox());
        mReplacementEditText.addTextChangedListener(new SimpleTextWatcher((SimpleTextWatcher.OnTextChangedListener) (s, start, before, count) -> onTextChanged()));
        customView(view, true);
        positiveText(com.stardust.autojs.R.string.ok);
        negativeText(com.stardust.autojs.R.string.cancel);
        title(R.string.text_find_or_replace);
    }


    private void storeState() {
        DefaultSharedPreferences.get(getContext()).edit()
                .putString(KEY_KEYWORDS, mKeywordsEditText.getText().toString())
                .apply();
    }


    private void restoreState() {
        mKeywordsEditText.setText(DefaultSharedPreferences.get(getContext())
                .getString(KEY_KEYWORDS, ""));
    }

    void syncWithReplaceCheckBox() {
        if (mReplaceAllCheckBox.isChecked() && !mReplaceCheckBox.isChecked()) {
            mReplaceCheckBox.setChecked(true);
        }
    }

    void onTextChanged() {
        if (mReplacementEditText.getText().length() > 0) {
            mReplaceCheckBox.setChecked(true);
        }
    }

    private void findOrReplace(MaterialDialog dialog) {
        String keywords = mKeywordsEditText.getText().toString();
        if (keywords.isEmpty()) {
            return;
        }
        try {
            boolean usingRegex = mRegexCheckBox.isChecked();
            if (!mReplaceCheckBox.isChecked()) {
                mEditorView.find(keywords, usingRegex);
            } else {
                String replacement = mReplacementEditText.getText().toString();
                if (mReplaceAllCheckBox.isChecked()) {
                    mEditorView.replaceAll(keywords, replacement, usingRegex);
                } else {
                    mEditorView.replace(keywords, replacement, usingRegex);
                }
            }
            dialog.dismiss();
        } catch (CodeEditor.CheckedPatternSyntaxException e) {
            e.printStackTrace();
            mKeywordsEditText.setError(getContext().getString(R.string.error_pattern_syntax));
        }

    }

    public FindOrReplaceDialogBuilder setQueryIfNotEmpty(String s) {
        if (!TextUtils.isEmpty(s))
            mKeywordsEditText.setText(s);
        return this;
    }
}
