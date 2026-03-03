package org.autojs.autojs.build

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.stardust.pio.UncheckedIOException
import com.stardust.util.DeveloperUtils
import org.autojs.autojs.BuildConfig
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

private val PackageInfo.versionCodeCompat: Long
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }

object ApkBuilderPluginHelper {

    private const val PLUGIN_PACKAGE_NAME = "org.autojs.apkbuilderplugin"
    private const val TEMPLATE_APK_PATH = "template.apk"

    fun isPluginAvailable(context: Context): Boolean {
        return DeveloperUtils.checkSignature(context, PLUGIN_PACKAGE_NAME)
    }

    fun openTemplateApk(context: Context, customTemplatePath: String = ""): InputStream {
        if (customTemplatePath.isNotBlank()) {
            val file = File(customTemplatePath)
            if (file.exists()) {
                return FileInputStream(file)
            }
            throw FileNotFoundException("Template APK not found: $customTemplatePath")
        }
        return try {
            context.assets.open(TEMPLATE_APK_PATH)
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    fun getPluginVersion(context: Context): Long {
        return try {
            val info = context.packageManager.getPackageInfo(PLUGIN_PACKAGE_NAME, 0)
            info.versionCodeCompat
        } catch (_: PackageManager.NameNotFoundException) {
            -1L
        }
    }

    fun getTemplateVersion(context: Context, customTemplatePath: String): Long {
        if (customTemplatePath.isEmpty()) return -1L
        val file = File(customTemplatePath)
        if (!file.exists()) return -1L
        val info = context.packageManager.getPackageArchiveInfo(customTemplatePath, 0) ?: return -1L
        return info.versionCodeCompat
    }

    fun getSuitableTemplateVersion(): Long {
        return BuildConfig.VERSION_CODE.toLong() - 200
    }
}
