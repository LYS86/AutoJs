package org.autojs.autojs.ui.project

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import org.autojs.autojs.build.ApkSigner
import com.stardust.autojs.project.ProjectConfig
import com.stardust.util.IntentUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.autojs.autojs.Pref
import org.autojs.autojs.R
import org.autojs.autojs.build.ApkBuilder
import org.autojs.autojs.build.ApkBuilderPluginHelper
import org.autojs.autojs.databinding.ActivityBuildBinding
import org.autojs.autojs.external.fileprovider.AppFileProvider
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.tool.BitmapTool
import org.autojs.autojs.ui.BaseActivity
import org.autojs.autojs.ui.filechooser.FileChooserDialogBuilder
import org.autojs.autojs.ui.shortcut.ShortcutIconSelectActivity
import java.io.File

class BuildActivity : BaseActivity(), ApkBuilder.ProgressCallback {

    private lateinit var binding: ActivityBuildBinding

    private var projectConfig: ProjectConfig? = null
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
    }

    private fun setupClickListeners() {
        binding.selectSource.setOnClickListener { selectSourceFilePath() }
        binding.selectOutput.setOnClickListener { selectOutputDirPath() }
        binding.selectTemplate.setOnClickListener { selectTemplateApk() }
        binding.selectKeystore.setOnClickListener { selectKeystoreFile() }
        binding.icon.setOnClickListener { selectIcon() }
        binding.fab.setOnClickListener { buildApk() }
        binding.btnInstall.setOnClickListener {
            val outApk = it.tag as? File ?: return@setOnClickListener
            IntentUtil.installApkOrToast(this, outApk.path, AppFileProvider.AUTHORITY)
        }
    }

    private fun selectTemplateApk() {
        val initialDir = if (binding.templatePath.text.toString().isNotEmpty()) {
            File(binding.templatePath.text.toString()).parent ?: Pref.getScriptDirPath()
        } else {
            Pref.getScriptDirPath()
        }
        FileChooserDialogBuilder(this)
            .title(R.string.text_template_apk_title)
            .dir(initialDir)
            .singleChoice { file ->
                if (file.name.endsWith(".apk", ignoreCase = true)) {
                    binding.templatePath.setText(file.path)
                } else {
                    Toast.makeText(this, R.string.text_invalid_package_name, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
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

    private fun selectKeystoreFile() {
        val initialDir = if (File(binding.keystorePath.text.toString()).exists()) {
            File(binding.keystorePath.text.toString()).parent ?: Pref.getScriptDirPath()
        } else {
            Pref.getScriptDirPath()
        }
        FileChooserDialogBuilder(this)
            .title(R.string.text_keystore_path)
            .dir(initialDir)
            .singleChoice { file ->
                binding.keystorePath.setText(file.path)
            }
            .show()
    }

    private fun buildApk() {
        if (checkInputs().not()) {
            return
        }
        val signConfigResult = getSignConfig()
        if (signConfigResult.isFailure) {
            val error = signConfigResult.exceptionOrNull() ?: Exception("Unknown error")
            showProgressCard()
            resetBuildSteps()
            setStepError(binding.iconPrepare, binding.textPrepare, error.message ?: "Unknown error")
            binding.fab.show()
            return
        }
        doBuildingApk(signConfigResult.getOrThrow())
    }

    private fun getSignConfig(): Result<ApkSigner.SignConfig> {
        val keystorePath = binding.keystorePath.text.toString()
        val keystorePassword = binding.keystorePassword.text.toString()
        val keyAlias = binding.keyAlias.text.toString()
        val keyPassword = binding.keyPassword.text.toString()

        if (keystorePath.isEmpty()) {
            return Result.failure(Exception(getString(R.string.text_keystore_path_empty)))
        }
        if (keystorePassword.isEmpty()) {
            return Result.failure(Exception(getString(R.string.text_keystore_password_empty)))
        }
        if (keyAlias.isEmpty()) {
            return Result.failure(Exception(getString(R.string.text_key_alias_empty)))
        }
        if (keyPassword.isEmpty()) {
            return Result.failure(Exception(getString(R.string.text_key_password_empty)))
        }

        val keystoreFile = File(keystorePath)
        if (!keystoreFile.exists()) {
            return Result.failure(Exception(getString(R.string.text_keystore_not_found)))
        }

        return Result.success(
            ApkSigner.SignConfig(
                keyStoreFile = keystoreFile,
                keyStorePassword = keystorePassword,
                keyAlias = keyAlias,
                keyPassword = keyPassword
            )
        )
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
    private fun doBuildingApk(signConfig: ApkSigner.SignConfig) {
        val appConfig = createAppConfig()
        val tmpDir = File(cacheDir, "build/")
        val outApk = File(
            binding.outputPath.text.toString(),
            "${appConfig.appName}_v${appConfig.versionName}.apk"
        )
        val templatePath = binding.templatePath.text.toString()
        showProgressCard()
        resetBuildSteps()
        Observable.fromCallable { callApkBuilder(tmpDir, outApk, appConfig, signConfig, templatePath) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { onBuildSuccessful(outApk) },
                { error -> onBuildFailed(error) }
            )
    }

    private fun showProgressCard() {
        binding.buildResultCard.visibility = View.VISIBLE
        binding.buildSuccessContent.visibility = View.GONE
        binding.fab.hide()
    }

    private fun resetBuildSteps() {
        val defaultColor = ContextCompat.getColor(this, android.R.color.tab_indicator_text)
        setStepPending(binding.iconPrepare)
        setStepPending(binding.iconBuild)
        setStepPending(binding.iconSign)
        setStepPending(binding.iconClean)
        binding.textPrepare.setText(R.string.apk_builder_prepare)
        binding.textPrepare.setTextColor(defaultColor)
        binding.textBuild.setText(R.string.apk_builder_build)
        binding.textBuild.setTextColor(defaultColor)
        binding.textSign.setText(R.string.apk_builder_package)
        binding.textSign.setTextColor(defaultColor)
        binding.textClean.setText(R.string.apk_builder_clean)
        binding.textClean.setTextColor(defaultColor)
    }

    private fun setStepPending(icon: ImageView) {
        icon.setImageResource(R.drawable.ic_build_step_pending)
    }

    private fun setStepSuccess(icon: ImageView) {
        icon.setImageResource(R.drawable.ic_build_step_success)
    }

    private fun setStepError(icon: ImageView, text: TextView, message: String) {
        icon.setImageResource(R.drawable.ic_build_step_error)
        text.text = message
        text.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
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
        return ApkBuilder.AppConfig(
            appName = appName,
            sourcePath = jsPath,
            packageName = packageName,
            versionCode = versionCode,
            versionName = versionName,
            icon = if (isDefaultIcon) null else {
                { BitmapTool.drawableToBitmap(binding.icon.drawable) }
            }
        )
    }

    private fun callApkBuilder(
        tmpDir: File,
        outApk: File,
        appConfig: ApkBuilder.AppConfig,
        signConfig: ApkSigner.SignConfig,
        templatePath: String
    ): ApkBuilder {
        val templateApk = ApkBuilderPluginHelper.openTemplateApk(this, templatePath)
        return ApkBuilder(templateApk, outApk, tmpDir)
            .setProgressCallback(this)
            .prepare()
            .withConfig(appConfig)
            .build()
            .sign(signConfig)
            .cleanWorkspace()
    }

    private fun onBuildFailed(error: Throwable) {
        val message = translateErrorMessage(error)
        setStepError(binding.iconPrepare, binding.textPrepare, message)
        binding.fab.show()
        Log.e(LOG_TAG, "Build failed", error)
    }

    private fun translateErrorMessage(error: Throwable): String {
        val originalMessage = error.message ?: return getString(R.string.text_build_error_unknown)
        return when {
            originalMessage.contains("PKCS12", ignoreCase = true) ||
            originalMessage.contains("wrong password", ignoreCase = true) ||
            originalMessage.contains("mac invalid", ignoreCase = true) -> {
                getString(R.string.text_keystore_password_error)
            }
            originalMessage.contains("PrivateKey", ignoreCase = true) ||
            originalMessage.contains("alias", ignoreCase = true) -> {
                getString(R.string.text_key_alias_error)
            }
            originalMessage.contains("Template APK not found", ignoreCase = true) -> {
                getString(R.string.text_template_not_found)
            }
            else -> originalMessage
        }
    }

    private fun onBuildSuccessful(outApk: File) {
        binding.buildSuccessContent.visibility = View.VISIBLE
        binding.buildResultPath.text = getString(R.string.format_build_successfully, outApk.path)
        binding.btnInstall.tag = outApk
        binding.fab.show()
    }

    override fun onPrepare(builder: ApkBuilder) {
        runOnUiThread {
            setStepSuccess(binding.iconPrepare)
            setStepPending(binding.iconBuild)
        }
    }

    override fun onBuild(builder: ApkBuilder) {
        runOnUiThread {
            setStepSuccess(binding.iconBuild)
            setStepPending(binding.iconSign)
        }
    }

    override fun onSign(builder: ApkBuilder) {
        runOnUiThread {
            setStepSuccess(binding.iconSign)
            setStepPending(binding.iconClean)
        }
    }

    override fun onClean(builder: ApkBuilder) {
        runOnUiThread {
            setStepSuccess(binding.iconClean)
        }
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
