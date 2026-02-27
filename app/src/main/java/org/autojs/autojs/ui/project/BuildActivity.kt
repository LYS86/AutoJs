package org.autojs.autojs.ui.project

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.textfield.TextInputLayout
import com.stardust.autojs.project.ProjectConfig
import com.stardust.util.IntentUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.autojs.build.ApkBuilder
import org.autojs.autojs.build.ApkBuilderPluginHelper
import org.autojs.autojs.databinding.ActivityBuildBinding
import org.autojs.autojs.external.fileprovider.AppFileProvider
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.tool.BitmapTool
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.filechooser.FileChooserDialogBuilder
import org.autojs.autojs.ui.shortcut.ShortcutIconSelectActivity
import java.io.File
import java.util.concurrent.Callable

class BuildActivity : BaseActivity(), ApkBuilder.ProgressCallback {

    private lateinit var binding: ActivityBuildBinding

    private var projectConfig: ProjectConfig? = null
    private var progressDialog: MaterialDialog? = null
    private var source: String? = null
    private var isDefaultIcon = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuildBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
    }

    private fun setupViews() {
        setToolbarAsBack(getString(R.string.text_build_apk))
        source = intent.getStringExtra(EXTRA_SOURCE)
        source?.let { setupWithSourceFile(ScriptFile(it)) }
        setupClickListeners()
        checkApkBuilderPlugin()
    }

    private fun setupClickListeners() {
        binding.selectSource.setOnClickListener { selectSourceFilePath() }
        binding.selectOutput.setOnClickListener { selectOutputDirPath() }
        binding.icon.setOnClickListener { selectIcon() }
        binding.fab.setOnClickListener { buildApk() }
    }

    private fun checkApkBuilderPlugin() {
        if (ApkBuilderPluginHelper.isPluginAvailable(this).not()) {
            showPluginDownloadDialog(R.string.no_apk_builder_plugin, true)
            return
        }
        val version = ApkBuilderPluginHelper.getPluginVersion(this)
        if (version < 0) {
            showPluginDownloadDialog(R.string.no_apk_builder_plugin, true)
            return
        }
        if (version < ApkBuilderPluginHelper.getSuitablePluginVersion()) {
            showPluginDownloadDialog(R.string.apk_builder_plugin_version_too_low, false)
        }
    }

    private fun showPluginDownloadDialog(msgRes: Int, finishIfCanceled: Boolean) {
        ThemeColorMaterialDialogBuilder(this)
            .content(msgRes)
            .positiveText(R.string.ok)
            .negativeText(R.string.cancel)
            .onPositive { _, _ -> downloadPlugin() }
            .onNegative { _, _ ->
                if (finishIfCanceled) finish()
            }
            .show()
    }

    private fun downloadPlugin() {
        val pluginVersion = ApkBuilderPluginHelper.getSuitablePluginVersion()
        IntentUtil.browse(this, "https://i.autojs.org/autojs/plugin/$pluginVersion.apk")
    }

    private fun setupWithSourceFile(file: ScriptFile) {
        var dir = file.parent
        if (dir.startsWith(filesDir.path)) {
            dir = Pref.getScriptDirPath()
        }
        binding.outputPath.setText(dir)
        binding.appName.setText(file.simplifiedName)
        binding.packageName.setText(getString(R.string.format_default_package_name, System.currentTimeMillis()))
        setSource(file)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    private fun selectSourceFilePath() {
        val initialDir = File(binding.sourcePath.text.toString()).parent
        FileChooserDialogBuilder(this)
            .title(R.string.text_source_file_path)
            .dir(
                Environment.getExternalStorageDirectory().path,
                initialDir ?: Pref.getScriptDirPath()
            )
            .singleChoice { setSource(it) }
            .show()
    }

    private fun setSource(file: File) {
        if (file.isDirectory.not()) {
            binding.sourcePath.setText(file.path)
            return
        }
        projectConfig = ProjectConfig.fromProjectDir(file.path) ?: return
        binding.outputPath.setText(File(source, projectConfig!!.buildDir).path)
        binding.appConfig.visibility = View.GONE
        binding.sourcePathContainer.visibility = View.GONE
    }

    private fun selectOutputDirPath() {
        val initialDir = if (File(binding.outputPath.text.toString()).exists()) {
            binding.outputPath.text.toString()
        } else {
            Pref.getScriptDirPath()
        }
        FileChooserDialogBuilder(this)
            .title(R.string.text_output_apk_path)
            .dir(initialDir)
            .chooseDir()
            .singleChoice { dir -> binding.outputPath.setText(dir.path) }
            .show()
    }

    private fun selectIcon() {
        startActivityForResult(Intent(this, ShortcutIconSelectActivity::class.java), REQUEST_CODE)
    }

    private fun buildApk() {
        if (ApkBuilderPluginHelper.isPluginAvailable(this).not()) {
            Toast.makeText(this, R.string.text_apk_builder_plugin_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        if (checkInputs().not()) {
            return
        }
        doBuildingApk()
    }

    private fun checkInputs(): Boolean {
        var inputValid = true
        inputValid = inputValid and checkNotEmpty(binding.sourcePath)
        inputValid = inputValid and checkNotEmpty(binding.outputPath)
        inputValid = inputValid and checkNotEmpty(binding.appName)
        inputValid = inputValid and checkNotEmpty(binding.sourcePath)
        inputValid = inputValid and checkNotEmpty(binding.versionCode)
        inputValid = inputValid and checkNotEmpty(binding.versionName)
        inputValid = inputValid and checkPackageNameValid(binding.packageName)
        return inputValid
    }

    private fun checkPackageNameValid(editText: EditText): Boolean {
        val text = editText.text
        val hint = (editText.parent.parent as TextInputLayout).hint.toString()
        if (text.isNullOrEmpty()) {
            editText.error = "$hint${getString(R.string.text_should_not_be_empty)}"
            return false
        }
        if (REGEX_PACKAGE_NAME.matches(text).not()) {
            editText.error = getString(R.string.text_invalid_package_name)
            return false
        }
        return true
    }

    private fun checkNotEmpty(editText: EditText): Boolean {
        if (editText.text.isNullOrEmpty().not() || editText.isShown.not()) return true
        val hint = (editText.parent.parent as TextInputLayout).hint.toString()
        editText.error = "$hint${getString(R.string.text_should_not_be_empty)}"
        return false
    }

    @SuppressLint("CheckResult")
    private fun doBuildingApk() {
        val appConfig = createAppConfig()
        val tmpDir = File(cacheDir, "build/")
        val outApk = File(
            binding.outputPath.text.toString(),
            "${appConfig.appName}_v${appConfig.versionName}.apk"
        )
        showProgressDialog()
        Observable.fromCallable { callApkBuilder(tmpDir, outApk, appConfig) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { onBuildSuccessful(outApk) },
                { error -> onBuildFailed(error) }
            )
    }

    private fun createAppConfig(): ApkBuilder.AppConfig {
        projectConfig?.let {
            return ApkBuilder.AppConfig.fromProjectConfig(source!!, it)
        }
        val jsPath = binding.sourcePath.text.toString()
        val versionName = binding.versionName.text.toString()
        val versionCode = binding.versionCode.text.toString().toInt()
        val appName = binding.appName.text.toString()
        val packageName = binding.packageName.text.toString()
        return ApkBuilder.AppConfig()
            .setAppName(appName)
            .setSourcePath(jsPath)
            .setPackageName(packageName)
            .setVersionCode(versionCode)
            .setVersionName(versionName)
            .setIcon(
                if (isDefaultIcon) null else {
                    Callable { BitmapTool.drawableToBitmap(binding.icon.drawable) }
                }
            )
    }

    private fun callApkBuilder(tmpDir: File, outApk: File, appConfig: ApkBuilder.AppConfig): ApkBuilder {
        val templateApk = ApkBuilderPluginHelper.openTemplateApk(this)
        return ApkBuilder(templateApk, outApk, tmpDir.path)
            .setProgressCallback(this)
            .prepare()
            .withConfig(appConfig)
            .build()
            .sign()
            .cleanWorkspace()
    }

    private fun showProgressDialog() {
        progressDialog = MaterialDialog.Builder(this)
            .progress(true, 100)
            .content(R.string.text_on_progress)
            .cancelable(false)
            .show()
    }

    private fun onBuildFailed(error: Throwable) {
        progressDialog?.dismiss()
        progressDialog = null
        Toast.makeText(this, "${getString(R.string.text_build_failed)}${error.message}", Toast.LENGTH_SHORT).show()
        Log.e(LOG_TAG, "Build failed", error)
    }

    private fun onBuildSuccessful(outApk: File) {
        progressDialog?.dismiss()
        progressDialog = null
        MaterialDialog.Builder(this)
            .title(R.string.text_build_successfully)
            .content(getString(R.string.format_build_successfully, outApk.path))
            .positiveText(R.string.text_install)
            .negativeText(R.string.cancel)
            .onPositive { _, _ ->
                IntentUtil.installApkOrToast(this, outApk.path, AppFileProvider.AUTHORITY)
            }
            .show()
    }

    override fun onPrepare(builder: ApkBuilder) {
        progressDialog?.setContent(R.string.apk_builder_prepare)
    }

    override fun onBuild(builder: ApkBuilder) {
        progressDialog?.setContent(R.string.apk_builder_build)
    }

    override fun onSign(builder: ApkBuilder) {
        progressDialog?.setContent(R.string.apk_builder_package)
    }

    override fun onClean(builder: ApkBuilder) {
        progressDialog?.setContent(R.string.apk_builder_clean)
    }

    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK) {
            return
        }
        ShortcutIconSelectActivity.getBitmapFromIntent(applicationContext, data!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { bitmap ->
                    binding.icon.setImageBitmap(bitmap)
                    isDefaultIcon = false
                },
                { it.printStackTrace() }
            )
    }

    companion object {
        private const val REQUEST_CODE = 44401
        @JvmField
        val EXTRA_SOURCE: String = "${BuildActivity::class.java.name}.extra_source_file"
        private const val LOG_TAG = "BuildActivity"
        private val REGEX_PACKAGE_NAME = Regex("^([A-Za-z][A-Za-z\\d_]*\\.)+([A-Za-z][A-Za-z\\d_]*)$")
    }
}
