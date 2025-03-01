package org.autojs.autojs.ui.project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputLayout;
import com.stardust.autojs.project.ProjectConfig;
import com.stardust.pio.PFiles;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.ActivityProjectConfigBinding;
import org.autojs.autojs.model.explorer.ExplorerDirPage;
import org.autojs.autojs.model.explorer.ExplorerFileItem;
import org.autojs.autojs.model.explorer.Explorers;
import org.autojs.autojs.model.project.ProjectTemplate;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.shortcut.ShortcutIconSelectActivity;
import org.autojs.autojs.ui.widget.SimpleTextWatcher;

import java.io.File;
import java.io.FileOutputStream;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ProjectConfigActivity extends BaseActivity {

    public static final String EXTRA_PARENT_DIRECTORY = "parent_directory";
    public static final String EXTRA_NEW_PROJECT = "new_project";
    public static final String EXTRA_DIRECTORY = "directory";
    private static final int REQUEST_CODE = 12477;
    private static final Pattern REGEX_PACKAGE_NAME = Pattern.compile("^([A-Za-z][A-Za-z\\d_]*\\.)+([A-Za-z][A-Za-z\\d_]*)$");

    private ActivityProjectConfigBinding binding;
    private File mDirectory;
    private File mParentDirectory;
    private ProjectConfig mProjectConfig;
    private boolean mNewProject;
    private Bitmap mIconBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProjectConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mNewProject = getIntent().getBooleanExtra(EXTRA_NEW_PROJECT, false);
        String parentDirectory = getIntent().getStringExtra(EXTRA_PARENT_DIRECTORY);
        if (mNewProject) {
            if (parentDirectory == null) {
                finish();
                return;
            }
            mParentDirectory = new File(parentDirectory);
            mProjectConfig = new ProjectConfig();
        } else {
            String dir = getIntent().getStringExtra(EXTRA_DIRECTORY);
            if (dir == null) {
                finish();
                return;
            }
            mDirectory = new File(dir);
            mProjectConfig = ProjectConfig.fromProjectDir(dir);
            if (mProjectConfig == null) {
                new ThemeColorMaterialDialogBuilder(this)
                        .title(R.string.text_invalid_project)
                        .positiveText(R.string.ok)
                        .dismissListener(dialogInterface -> finish())
                        .show();
            }
        }

        setupViews();
        binding.fab.setOnClickListener(v -> commit());
        binding.icon.setOnClickListener(v -> selectIcon());
    }

    void setupViews() {
        if (mProjectConfig == null) return;
        setToolbarAsBack(mNewProject ? getString(R.string.text_new_project) : mProjectConfig.getName());
        if (mNewProject) {
            binding.appName.addTextChangedListener(new SimpleTextWatcher(s ->
                    binding.projectLocation.setText(new File(mParentDirectory, s.toString()).getPath()))
            );
        } else {
            binding.appName.setText(mProjectConfig.getName());
            binding.versionCode.setText(String.valueOf(mProjectConfig.getVersionCode()));
            binding.packageName.setText(mProjectConfig.getPackageName());
            binding.versionName.setText(mProjectConfig.getVersionName());
            binding.mainFileName.setText(mProjectConfig.getMainScriptFile());
            binding.projectLocation.setVisibility(View.GONE);
            String icon = mProjectConfig.getIcon();
            if (icon != null) {
                Glide.with(this)
                        .load(new File(mDirectory, icon))
                        .into(binding.icon);
            }
        }
    }

    @SuppressLint("CheckResult")
    void commit() {
        if (!checkInputs()) return;
        syncProjectConfig();
        if (mIconBitmap != null) {
            saveIcon(mIconBitmap)
                    .subscribe(ignored -> saveProjectConfig(), e -> {
                        e.printStackTrace();
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            saveProjectConfig();
        }
    }

    @SuppressLint("CheckResult")
    private void saveProjectConfig() {
        if (mNewProject) {
            new ProjectTemplate(mProjectConfig, mDirectory)
                    .newProject()
                    .subscribe(ignored -> {
                        Explorers.workspace().notifyChildrenChanged(new ExplorerDirPage(mParentDirectory, null));
                        finish();
                    }, e -> {
                        e.printStackTrace();
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Observable.fromCallable(() -> {
                        PFiles.write(ProjectConfig.configFileOfDir(mDirectory.getPath()),
                                mProjectConfig.toJson());
                        return Void.TYPE;
                    })
                    .observeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(ignored -> {
                        ExplorerFileItem item = new ExplorerFileItem(mDirectory, null);
                        Explorers.workspace().notifyItemChanged(item, item);
                        finish();
                    }, e -> {
                        e.printStackTrace();
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    void selectIcon() {
        startActivityForResult(new Intent(this, ShortcutIconSelectActivity.class), REQUEST_CODE);
    }

    private void syncProjectConfig() {
        mProjectConfig.setName(binding.appName.getText().toString());
        mProjectConfig.setVersionCode(Integer.parseInt(binding.versionCode.getText().toString()));
        mProjectConfig.setVersionName(binding.versionName.getText().toString());
        mProjectConfig.setMainScriptFile(binding.mainFileName.getText().toString());
        mProjectConfig.setPackageName(binding.packageName.getText().toString());
        if (mNewProject) {
            String location = binding.projectLocation.getText().toString();
            mDirectory = new File(location);
        }
    }

    private boolean checkInputs() {
        boolean inputValid = true;
        inputValid &= checkNotEmpty(binding.appName);
        inputValid &= checkNotEmpty(binding.versionCode);
        inputValid &= checkNotEmpty(binding.versionName);
        inputValid &= checkPackageNameValid(binding.packageName);
        return inputValid;
    }

    private boolean checkPackageNameValid(EditText editText) {
        Editable text = editText.getText();
        String hint = ((TextInputLayout) editText.getParent().getParent()).getHint().toString();
        if (TextUtils.isEmpty(text)) {
            editText.setError(hint + getString(R.string.text_should_not_be_empty));
            return false;
        }
        if (!REGEX_PACKAGE_NAME.matcher(text).matches()) {
            editText.setError(getString(R.string.text_invalid_package_name));
            return false;
        }
        return true;
    }

    private boolean checkNotEmpty(EditText editText) {
        if (!TextUtils.isEmpty(editText.getText())) return true;
        String hint = ((TextInputLayout) editText.getParent().getParent()).getHint().toString();
        editText.setError(hint + getString(R.string.text_should_not_be_empty));
        return false;
    }

    @SuppressLint("CheckResult")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        ShortcutIconSelectActivity.getBitmapFromIntent(getApplicationContext(), data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(bitmap -> {
                            binding.icon.setImageBitmap(bitmap);
                            mIconBitmap = bitmap;
                        },
                        Throwable::printStackTrace);
    }

    @SuppressLint("CheckResult")
    private Observable<String> saveIcon(Bitmap b) {
        return Observable.just(b)
                .map(bitmap -> {
                    String iconPath = mProjectConfig.getIcon();
                    if (iconPath == null) iconPath = "res/logo.png";
                    File iconFile = new File(mDirectory, iconPath);
                    PFiles.ensureDir(iconFile.getPath());
                    FileOutputStream fos = new FileOutputStream(iconFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    return iconPath;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(iconPath -> mProjectConfig.setIcon(iconPath));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}