package com.stardust.autojs.runtime.api

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stardust.autojs.annotation.ScriptInterface
import com.stardust.autojs.core.compat.getApplicationInfoCompat
import com.stardust.autojs.core.compat.getInstalledApplicationsCompat
import com.stardust.autojs.core.compat.getPackageInfoCompat
import com.stardust.autojs.core.compat.versionCodeCompat
import com.stardust.util.IntentUtil
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

class AppUtils @JvmOverloads constructor(
    private val mContext: Context,
    private val mFileProviderAuthority: String? = null
) {

    @Volatile
    private var mCurrentActivity: WeakReference<Activity?> = WeakReference(null)

    @ScriptInterface
    fun launchPackage(packageName: String): Boolean {
        return try {
            val packageManager = mContext.packageManager
            mContext.startActivity(
                packageManager.getLaunchIntentForPackage(packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    @ScriptInterface
    fun sendLocalBroadcastSync(intent: Intent) {
        LocalBroadcastManager.getInstance(mContext).sendBroadcastSync(intent)
    }

    @ScriptInterface
    fun launchApp(appName: String): Boolean {
        val pkg = getPackageName(appName) ?: return false
        return launchPackage(pkg)
    }

    @ScriptInterface
    fun getPackageName(appName: String): String? {
        val packageManager = mContext.packageManager
        val installedApplications = packageManager.getInstalledApplicationsCompat(PackageManager.GET_META_DATA)
        for (applicationInfo in installedApplications) {
            if (packageManager.getApplicationLabel(applicationInfo).toString() == appName) {
                return applicationInfo.packageName
            }
        }
        return null
    }

    @ScriptInterface
    fun getAppName(packageName: String): String? {
        val packageManager = mContext.packageManager
        return try {
            val applicationInfo = getApplicationInfo(packageName)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    @ScriptInterface
    fun openAppSetting(packageName: String): Boolean {
        return try {
            getApplicationInfo(packageName)
            IntentUtil.goToAppDetailSettings(mContext, packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    @ScriptInterface
    fun getFileProviderAuthority(): String? = mFileProviderAuthority

    fun getCurrentActivity(): Activity? {
        Timber.d("getCurrentActivity: ${mCurrentActivity.get()}")
        return mCurrentActivity.get()
    }

    @ScriptInterface
    fun uninstall(packageName: String) {
        mContext.startActivity(
            Intent(Intent.ACTION_DELETE, "package:$packageName".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    @ScriptInterface
    fun viewFile(path: String?) {
        if (path == null) throw NullPointerException("path == null")
        IntentUtil.viewFile(mContext, path, mFileProviderAuthority)
    }

    @ScriptInterface
    fun editFile(path: String?) {
        if (path == null) throw NullPointerException("path == null")
        IntentUtil.editFile(mContext, path, mFileProviderAuthority)
    }

    @ScriptInterface
    fun openUrl(url: String) {
        var finalUrl = url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            finalUrl = "http://$url"
        }
        mContext.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setData(finalUrl.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    @ScriptInterface
    fun parseUri(uri: String): Uri {
        if (uri.startsWith("file://")) {
            return getUriForFile(uri.substring(7))
        }
        return uri.toUri()
    }

    @ScriptInterface
    fun getUriForFile(path: String): Uri {
        var filePath = path
        if (filePath.startsWith("file://")) {
            filePath = filePath.substring(7)
        }
        val file = File(filePath)
        if (mFileProviderAuthority == null) {
            return Uri.fromFile(file)
        }
        return FileProvider.getUriForFile(mContext, mFileProviderAuthority, file)
    }

    val versionCode: Long
        get() = try {
            val packageInfo = getPackageInfo()
            packageInfo.versionCodeCompat
        } catch (_: PackageManager.NameNotFoundException) {
            -1L
        }

    val versionName: String
        get() = try {
            val packageInfo = getPackageInfo()
            packageInfo.versionName ?: "unknown"
        } catch (_: PackageManager.NameNotFoundException) {
            "unknown"
        }

    private fun getPackageInfo(): PackageInfo {
        return mContext.packageManager.getPackageInfoCompat(mContext.packageName)
    }

    private fun getApplicationInfo(packageName: String): ApplicationInfo {
        return mContext.packageManager.getApplicationInfoCompat(packageName)
    }

    fun setCurrentActivity(currentActivity: Activity?) {
        mCurrentActivity = WeakReference(currentActivity)
        Timber.d("setCurrentActivity: $currentActivity")
    }
}
