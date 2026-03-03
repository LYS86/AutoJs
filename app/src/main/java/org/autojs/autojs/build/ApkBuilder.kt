package org.autojs.autojs.build

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.apkbuilder.ManifestEditor
import com.stardust.autojs.project.BuildInfo
import com.stardust.autojs.project.ProjectConfig
import com.stardust.autojs.script.EncryptedScriptFileHeader
import com.stardust.autojs.script.JavaScriptFileSource
import com.stardust.util.AdvancedEncryptionStandard
import com.stardust.util.MD5
import zhao.arsceditor.ResDecoder.ARSCDecoder
import zhao.arsceditor.ResDecoder.data.ResTable
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ApkBuilder(
    private val apkInputStream: InputStream,
    private val outApkFile: File,
    private val workspaceDir: File
) {
    private var progressCallback: ProgressCallback? = null
    private var arscPackageName: String? = null
    private var manifestEditor: ManifestEditor? = null
    private var appConfig: AppConfig? = null
    private var initVector: String? = null
    private var key: String? = null

    init {
        outApkFile.parentFile?.mkdirs()
    }

    private val manifestFile: File
        get() = workspaceDir.resolve("AndroidManifest.xml")

    fun setProgressCallback(callback: ProgressCallback?) = apply { progressCallback = callback }

    fun prepare() = apply {
        progressCallback?.let { callback ->
            GlobalAppContext.post { callback.onPrepare(this@ApkBuilder) }
        }
        unzipApk()
    }

    private fun unzipApk() {
        workspaceDir.apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }
        ZipInputStream(apkInputStream).use { zis ->
            generateSequence { zis.nextEntry }
                .filterNot { it.isDirectory || it.name.startsWith("META-INF/") }
                .forEach { entry ->
                    workspaceDir.resolve(entry.name).also { file ->
                        if (file.exists() && file.isDirectory) return@forEach
                        file.parentFile?.mkdirs()
                        file.outputStream().use { fos -> zis.copyTo(fos) }
                    }
                }
        }
    }

    fun setScriptFile(path: String) = apply {
        if (File(path).isDirectory) {
            copyDir("assets/project/", path)
        } else {
            replaceFile("assets/project/main.js", path)
        }
    }

    fun copyDir(relativePath: String, path: String) {
        val fromDir = File(path)
        val toDir = workspaceDir.resolve(relativePath)
        toDir.mkdir()
        fromDir.listFiles()?.forEach { child ->
            when {
                child.isFile -> {
                    if (child.name.endsWith(".js")) {
                        encrypt(toDir, child)
                    } else {
                        child.copyTo(toDir.resolve(child.name), overwrite = true)
                    }
                }

                appConfig?.ignoredDirs?.contains(child) != true -> {
                    copyDir(File(relativePath, child.name).path + "/", child.path)
                }
            }
        }
    }

    private fun encrypt(toDir: File, file: File) {
        toDir.resolve(file.name).outputStream().use { fos ->
            encrypt(fos, file)
        }
    }

    private fun encrypt(fos: OutputStream, file: File) {
        val currentKey = checkNotNull(key) { "Encryption key not initialized. Call withConfig() first." }
        val currentVector = checkNotNull(initVector) { "Init vector not initialized. Call withConfig() first." }
        try {
            EncryptedScriptFileHeader.writeHeader(
                fos,
                JavaScriptFileSource(file).executionMode.toShort()
            )
            val bytes = AdvancedEncryptionStandard(
                currentKey.toByteArray(),
                currentVector
            ).encrypt(file.readBytes())
            fos.write(bytes)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun replaceFile(relativePath: String, newFilePath: String) = apply {
        val targetFile = workspaceDir.resolve(relativePath)
        if (newFilePath.endsWith(".js")) {
            targetFile.parentFile?.mkdirs()
            targetFile.outputStream().use { fos ->
                encrypt(fos, File(newFilePath))
            }
        } else {
            File(newFilePath).copyTo(targetFile, overwrite = true)
        }
    }

    fun withConfig(config: AppConfig) = apply {
        appConfig = config
        manifestEditor = ManifestEditor(manifestFile).withConfig(ManifestEditor.Config(
            appName = config.appName,
            versionName = config.versionName,
            versionCode = config.versionCode,
            packageName = config.packageName,
            authorities = "${config.packageName}.fileprovider"
        ))
        arscPackageName = config.packageName
        updateProjectConfig(config)
        if (config.sourcePath.isNotEmpty()) setScriptFile(config.sourcePath)
    }

    private fun updateProjectConfig(config: AppConfig) {
        val projectConfig: ProjectConfig = if (File(config.sourcePath).isDirectory) {
            ProjectConfig.fromProjectDir(config.sourcePath)
                .also { cfg ->
                    cfg.buildInfo = BuildInfo.generate(cfg.buildInfo.buildNumber + 1)
                    File(ProjectConfig.configFileOfDir(config.sourcePath)).writeText(cfg.toJson())
                }
        } else {
            ProjectConfig()
                .setMainScriptFile("main.js")
                .setName(config.appName)
                .setPackageName(config.packageName)
                .setVersionName(config.versionName)
                .setVersionCode(config.versionCode)
                .also {
                    it.buildInfo = BuildInfo.generate(config.versionCode.toLong())
                    workspaceDir.resolve("assets/project/project.json").writeText(it.toJson())
                }
        }
        key = MD5.md5(projectConfig.packageName + projectConfig.versionName + projectConfig.mainScriptFile)
        initVector = MD5.md5(projectConfig.buildInfo.buildId + projectConfig.name).substring(0, 16)
    }

    fun build() = apply {
        progressCallback?.let { callback ->
            GlobalAppContext.post { callback.onBuild(this@ApkBuilder) }
        }
        manifestEditor?.commit()
        appConfig?.icon?.invoke()?.let { bitmap ->
            workspaceDir.resolve("res/mipmap/ic_launcher.png").outputStream().use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
        }
        manifestFile.outputStream().use { manifestEditor?.writeTo(it) }
        arscPackageName?.let { buildArsc() }
    }

    fun sign(config: ApkSigner.SignConfig) = apply {
        progressCallback?.let { callback ->
            GlobalAppContext.post { callback.onSign(this@ApkBuilder) }
        }
        val tempApk = File.createTempFile("apk", ".tmp", outApkFile.parentFile)
        try {
            zipDirectory(workspaceDir, tempApk)
            ApkSigner.sign(tempApk, outApkFile, config)
        } finally {
            tempApk.delete()
        }
    }

    private fun zipDirectory(dir: File, output: File) {
        ZipOutputStream(output.outputStream()).use { zos ->
            zipDirectoryInternal(dir, "", zos)
        }
    }

    private fun zipDirectoryInternal(dir: File, prefix: String, zos: ZipOutputStream) {
        dir.listFiles()?.forEach { file ->
            val entryName = if (prefix.isEmpty()) file.name else "$prefix/${file.name}"
            if (file.isDirectory) {
                zos.putNextEntry(ZipEntry("$entryName/"))
                zos.closeEntry()
                zipDirectoryInternal(file, entryName, zos)
            } else {
                zos.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { fis -> fis.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    fun cleanWorkspace() = apply {
        progressCallback?.let { callback ->
            GlobalAppContext.post { callback.onClean(this@ApkBuilder) }
        }
        workspaceDir.deleteRecursively()
    }

    private fun buildArsc() {
        val oldArsc = workspaceDir.resolve("resources.arsc")
        val newArsc = workspaceDir.resolve("resources.arsc.new")
        val decoder = ARSCDecoder(
            BufferedInputStream(FileInputStream(oldArsc)),
            null as ResTable?,
            false
        )
        newArsc.outputStream().use { fos ->
            decoder.CloneArsc(fos, arscPackageName, true)
        }
        oldArsc.delete()
        newArsc.renameTo(oldArsc)
    }

    interface ProgressCallback {
        fun onPrepare(builder: ApkBuilder)
        fun onBuild(builder: ApkBuilder)
        fun onSign(builder: ApkBuilder)
        fun onClean(builder: ApkBuilder)
    }

    data class AppConfig(
        val appName: String,
        val versionName: String,
        val versionCode: Int,
        val sourcePath: String,
        val packageName: String,
        val ignoredDirs: List<File> = emptyList(),
        val icon: (() -> Bitmap)? = null
    ) {
        fun ignoreDir(dir: File) = copy(ignoredDirs = ignoredDirs + dir)

        fun setIcon(icon: (() -> Bitmap)?) = copy(icon = icon)

        fun setIcon(iconPath: String?) = iconPath?.let {
            copy(icon = { BitmapFactory.decodeFile(it) })
        } ?: this

        companion object {
            fun fromProjectConfig(projectDir: String, projectConfig: ProjectConfig): AppConfig {
                return AppConfig(
                    appName = projectConfig.name.orEmpty(),
                    packageName = projectConfig.packageName.orEmpty(),
                    versionCode = projectConfig.versionCode,
                    versionName = projectConfig.versionName.orEmpty(),
                    sourcePath = projectDir
                ).also { config ->
                    projectConfig.icon?.let { config.setIcon(File(projectDir, it).path) }
                    config.ignoreDir(File(projectDir, projectConfig.buildDir))
                }
            }
        }
    }
}
