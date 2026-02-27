package org.autojs.autojs.ui.project

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputLayout
import com.stardust.autojs.project.ProjectConfig
import com.stardust.pio.PFiles
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.R
import org.autojs.autojs.databinding.ActivityProjectConfigBinding
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerFileItem
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.project.ProjectTemplate
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.shortcut.ShortcutIconSelectActivity
import org.autojs.autojs.ui.widget.SimpleTextWatcher
import java.io.File
import java.io.FileOutputStream

class ProjectConfigActivity : BaseActivity() {

    companion object {
        const val EXTRA_PARENT_DIRECTORY = "parent_directory"
        const val EXTRA_NEW_PROJECT = "new_project"
        const val EXTRA_DIRECTORY = "directory"

        private const val REQUEST_CODE = 12477
        private val REGEX_PACKAGE_NAME = Regex("^([A-Za-z][A-Za-z\\d_]*\\.)+([A-Za-z][A-Za-z\\d_]*)$")
    }

    private lateinit var binding: ActivityProjectConfigBinding

    private var directory: File? = null
    private var parentDirectory: File? = null
    private var projectConfig: ProjectConfig? = null
    private var newProject = false
    private var iconBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjectConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        newProject = intent.getBooleanExtra(EXTRA_NEW_PROJECT, false)
        val parentDir = intent.getStringExtra(EXTRA_PARENT_DIRECTORY)
        if (newProject) {
            if (parentDir == null) {
                finish()
                return
            }
            parentDirectory = File(parentDir)
            projectConfig = ProjectConfig()
        } else {
            val dir = intent.getStringExtra(EXTRA_DIRECTORY)
            if (dir == null) {
                finish()
                return
            }
            directory = File(dir)
            projectConfig = ProjectConfig.fromProjectDir(dir)
            if (projectConfig == null) {
                ThemeColorMaterialDialogBuilder(this)
                    .title(R.string.text_invalid_project)
                    .positiveText(R.string.ok)
                    .dismissListener { finish() }
                    .show()
            }
        }

        setupViews()
    }

    private fun setupViews() {
        val config = projectConfig ?: return
        setToolbarAsBack(if (newProject) getString(R.string.text_new_project) else config.name)
        if (newProject) {
            binding.appName.addTextChangedListener(
                SimpleTextWatcher { s ->
                    binding.projectLocation.setText(File(parentDirectory, s.toString()).path)
                }
            )
        } else {
            binding.appName.setText(config.name)
            binding.versionCode.setText(config.versionCode.toString())
            binding.packageName.setText(config.packageName)
            binding.versionName.setText(config.versionName)
            binding.mainFileName.setText(config.mainScriptFile)
            binding.projectLocation.visibility = View.GONE
            val icon = config.icon
            if (icon != null) {
                directory?.let { dir ->
                    Glide.with(this)
                        .load(File(dir, icon))
                        .into(binding.icon)
                }
            }
        }

        binding.fab.setOnClickListener { commit() }
        binding.icon.setOnClickListener { selectIcon() }
    }

    @SuppressLint("CheckResult")
    private fun commit() {
        if (!checkInputs()) {
            return
        }
        syncProjectConfig()
        val bitmap = iconBitmap
        if (bitmap != null) {
            saveIcon(bitmap)
                .subscribe({ saveProjectConfig() }) { e ->
                    e.printStackTrace()
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
        } else {
            saveProjectConfig()
        }
    }

    @SuppressLint("CheckResult")
    private fun saveProjectConfig() {
        val config = projectConfig ?: return
        val dir = directory ?: return
        val parent = parentDirectory

        if (newProject && parent != null) {
            ProjectTemplate(config, dir)
                .newProject()
                .subscribe({
                    Explorers.workspace().notifyChildrenChanged(ExplorerDirPage(parent, null))
                    finish()
                }) { e ->
                    e.printStackTrace()
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
        } else {
            Observable.fromCallable {
                PFiles.write(ProjectConfig.configFileOfDir(dir.path), config.toJson())
                Void.TYPE
            }
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    val item = ExplorerFileItem(dir, null)
                    Explorers.workspace().notifyItemChanged(item, item)
                    finish()
                }) { e ->
                    e.printStackTrace()
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun selectIcon() {
        val intent = Intent(this, ShortcutIconSelectActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE)
    }

    private fun syncProjectConfig() {
        val config = projectConfig ?: return
        config.name = binding.appName.text.toString()
        config.versionCode = binding.versionCode.text.toString().toInt()
        config.versionName = binding.versionName.text.toString()
        config.mainScriptFile = binding.mainFileName.text.toString()
        config.packageName = binding.packageName.text.toString()
        if (newProject) {
            val location = binding.projectLocation.text.toString()
            directory = File(location)
        }
    }

    private fun checkInputs(): Boolean {
        var inputValid = true
        inputValid = inputValid && checkNotEmpty(binding.appName)
        inputValid = inputValid && checkNotEmpty(binding.versionCode)
        inputValid = inputValid && checkNotEmpty(binding.versionName)
        inputValid = inputValid && checkPackageNameValid(binding.packageName)
        return inputValid
    }

    private fun checkPackageNameValid(editText: android.widget.EditText): Boolean {
        val text = editText.text
        val hint = (editText.parent.parent as TextInputLayout).hint.toString()
        if (text.isNullOrEmpty()) {
            editText.error = hint + getString(R.string.text_should_not_be_empty)
            return false
        }
        if (!REGEX_PACKAGE_NAME.matches(text)) {
            editText.error = getString(R.string.text_invalid_package_name)
            return false
        }
        return true
    }

    private fun checkNotEmpty(editText: android.widget.EditText): Boolean {
        if (editText.text.isNullOrEmpty().not()) {
            return true
        }
        val hint = (editText.parent.parent as TextInputLayout).hint.toString()
        editText.error = hint + getString(R.string.text_should_not_be_empty)
        return false
    }

    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK) {
            return
        }
        ShortcutIconSelectActivity.getBitmapFromIntent(applicationContext, data)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ bitmap ->
                binding.icon.setImageBitmap(bitmap)
                iconBitmap = bitmap
            }) { obj ->
                obj.printStackTrace()
            }
    }

    @SuppressLint("CheckResult")
    private fun saveIcon(b: Bitmap): Observable<String> {
        val config = projectConfig ?: return Observable.error(IllegalStateException("ProjectConfig is null"))
        val dir = directory ?: return Observable.error(IllegalStateException("Directory is null"))

        return Observable.just(b)
            .map { bitmap ->
                var iconPath = config.icon
                if (iconPath == null) {
                    iconPath = "res/logo.png"
                }
                val iconFile = File(dir, iconPath)
                PFiles.ensureDir(iconFile.path)
                FileOutputStream(iconFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                iconPath
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { iconPath -> config.icon = iconPath }
    }
}
