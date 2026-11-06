package org.autojs.autojs.external.shortcut

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import com.stardust.util.IntentUtil
import java.lang.reflect.Method
import timber.log.Timber

private val isMiui: Boolean by lazy {
    Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) &&
            runCatching { Class.forName("miui.os.Build") }.isSuccess
}

// 公开 API 只能按字符串 op 名查询，而 MIUI 的这个 op 没有注册名字，只剩数字版入口
@SuppressLint("DiscouragedPrivateApi")
private fun findCheckOpNoThrow(): Method? = runCatching {
    AppOpsManager::class.java.getDeclaredMethod(
        "checkOpNoThrow",
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        String::class.java,
    ).apply { isAccessible = true }
}.getOrNull()

/**
 * 是否有 MIUI 的「桌面快捷方式」权限。
 * 非 MIUI 直接放行；MIUI 上取不到这个 op（版本改动）也按放行处理，
 * 不能把「查不到」当成「被拒绝」。
 */
fun Context.hasMIUIShortcutPermission(): Boolean {
    if (!isMiui) {
        return true
    }
    val method = findCheckOpNoThrow() ?: return true
    // MIUI 的 OP_INSTALL_SHORTCUT 随 SDK 分支取值，23 起是自定义 op 10017，21/22 用 63
    val op = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> 10017
        else -> 63
    }
    val mode = runCatching {
        method.invoke(
            getSystemService(Context.APP_OPS_SERVICE),
            op,
            Process.myUid(),
            packageName,
        ) as Int
    }.getOrElse {
        Timber.w(it, "读取 MIUI 桌面快捷方式权限失败，按有权限处理")
        return true
    }
    // MIUI 上 DEFAULT/IGNORED/ERRORED 都表现为创建请求被静默丢弃
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * 打开 MIUI 安全中心里本应用的权限页。
 */
fun Context.openAppPermissionSettings(): Boolean {
    val intents = listOf(
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity",
            )
        ),
        Intent("miui.intent.action.APP_PERM_EDITOR"),
    )
    for (intent in intents) {
        intent.putExtra("extra_pkgname", packageName)
        if (this !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { startActivity(intent) }.isSuccess) {
            return true
        }
    }
    return IntentUtil.goToAppDetailSettings(this)
}
