package com.stardust.autojs.core.compat

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        }

        else -> {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, 0)
        }
    }
}

fun PackageManager.getApplicationInfoCompat(packageName: String): ApplicationInfo {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        }

        else -> {
            @Suppress("DEPRECATION")
            getApplicationInfo(packageName, 0)
        }
    }
}

fun PackageManager.getInstalledApplicationsCompat(flags: Int): List<ApplicationInfo> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        }

        else -> {
            @Suppress("DEPRECATION")
            getInstalledApplications(flags)
        }
    }
}

val PackageInfo.versionCodeCompat: Long
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }

fun PackageManager.queryIntentActivitiesCompat(intent: Intent, flags: Int): List<ResolveInfo> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        }

        else -> {
            @Suppress("DEPRECATION")
            queryIntentActivities(intent, flags)
        }
    }
}
