package org.autojs.autojs.ui.floating

import android.Manifest.permission.SYSTEM_ALERT_WINDOW
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.permission.PermissionManager
import com.stardust.enhancedfloaty.FloatyService
import com.stardust.enhancedfloaty.FloatyWindow
import org.autojs.autojs.PrefV2
import org.autojs.autojs.R
import org.autojs.autojs.tool.PermissionTool
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * 悬浮窗管理器
 * @author Stardust (2017/9/30),Lin
 */
object FloatyWindowManager {
    const val KEY_MENU_SHOWN = "key_floating_menu_shown"

    var isMenuShown: Boolean by PrefV2.boolean(KEY_MENU_SHOWN, false)

    private var menuRef: WeakReference<CircularMenu>? = null


    /**
     * 添加悬浮窗到系统窗口
     */
    fun addWindow(context: Context, window: FloatyWindow): Boolean {
        if (!hasPermission(context)) {
            context.showToast(R.string.text_no_floating_window_permission)
            return false
        }

        return try {
            context.startService(Intent(context, FloatyService::class.java))
            FloatyService.addWindow(window)
            true
        } catch (e: Exception) {
            Timber.e(e, "添加悬浮窗失败")
            false
        }
    }



    /**
     * 检查悬浮菜单是否正在显示
     */
    private fun isMenuShowing(): Boolean {
        return menuRef?.get() != null
    }

    /**
     * 恢复显示
     */
    fun restore(): Boolean {
        return isMenuShown && showMenu()
    }


    /**
     * 显示悬浮菜单
     */
    fun showMenu(): Boolean {
        val context = GlobalAppContext.get()
        if (!hasPermission(context)) {
            context.showToast(R.string.text_no_floating_window_permission)
            return false
        }
        if (isMenuShowing()) {
            return true
        }
        context.startService(Intent(context, FloatyService::class.java))
        CircularMenu(context).also { menu ->
            menuRef = WeakReference(menu)
        }
        isMenuShown = true
        return true
    }

    /**
     * 检查是否有悬浮窗权限
     */
    fun hasPermission(context: Context): Boolean {
        return PermissionManager.hasPermission(context, SYSTEM_ALERT_WINDOW)
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestPermission(
        activity: Activity, onSuccess: (() -> Unit)? = null, onError: ((Throwable) -> Unit)? = null
    ) {
        if (hasPermission(activity)) {
            onSuccess?.invoke()
            return
        }

        PermissionTool().apply {
            add(task = {
                activity.showToast(R.string.text_no_floating_window_permission)
                PermissionManager.requestPermission(activity, SYSTEM_ALERT_WINDOW)
            }, onError = { error ->
                onError?.invoke(error)
            })
            check(task = {
                hasPermission(activity)
            }, onSuccess = {
                onSuccess?.invoke()
            }, onError = { error ->
                onError?.invoke(error)
            }, timeout = 20_000L)
            start()
        }
    }

    /**
     * 隐藏悬浮菜单
     */
    fun hideMenu() {
        menuRef?.get()?.close()
        menuRef?.clear()
        menuRef = null
        isMenuShown = false
    }

    /**
     * 获取当前系统适用的悬浮窗窗口类型
     */
    @Suppress("DEPRECATION")
    fun getWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
    }

    private fun Context.showToast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }
}