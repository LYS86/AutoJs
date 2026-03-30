package com.stardust.autojs.runtime.api

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.Nullable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stardust.autojs.annotation.ScriptInterface
import com.stardust.util.IntentUtil
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
        val installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
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

    @Nullable
    fun getCurrentActivity(): Activity? {
        Log.d("App", "getCurrentActivity: ${mCurrentActivity.get()}")
        return mCurrentActivity.get()
    }

    @ScriptInterface
    fun uninstall(packageName: String) {
        mContext.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
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
                .setData(Uri.parse(finalUrl))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    val versionCode: Long
        get() = try {
            val packageInfo = getPackageInfo()
            packageInfo.longVersionCode
        } catch (_: PackageManager.NameNotFoundException) {
            -1L
        }

    val versionName: String
        get() = try {
            val packageInfo = getPackageInfo()
            packageInfo.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            "unknown"
        }

    private fun getPackageInfo(): PackageInfo {
        val packageManager = mContext.packageManager
        val packageName = mContext.packageName
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            }

            else -> {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        }
    }

    private fun getApplicationInfo(packageName: String): ApplicationInfo {
        val packageManager = mContext.packageManager
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            }

            else -> {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
        }
    }

    fun setCurrentActivity(currentActivity: Activity?) {
        mCurrentActivity = WeakReference(currentActivity)
        Log.d("App", "setCurrentActivity: $currentActivity")
    }
}
