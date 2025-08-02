package org.autojs.autojs.external.shortcut

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.autojs.autojs.external.ScriptIntents

object ShortcutManager {

    /**
     * 创建固定快捷方式
     * @param context 上下文
     * @param name 快捷方式名称
     * @param id 快捷方式ID
     * @param icon 快捷方式图标
     * @param targetActivity 目标活动
     * @param scriptPath 脚本路径
     */
    @JvmStatic
    fun createPinnedShortcut(
        context: Context,
        name: CharSequence,
        id: String,
        icon: Icon,
        targetActivity: Class<*>,
        scriptPath: String?
    ): Boolean {
        val isSupport = ShortcutManagerCompat.isRequestPinShortcutSupported(context)
        if (!isSupport) {
            return false
        }
        val shortcuts =
            ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)

        val launchIntent = Intent(context, targetActivity).apply {
            action = Intent.ACTION_VIEW
            scriptPath?.let { putExtra(ScriptIntents.EXTRA_KEY_PATH, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val shortcut =
            ShortcutInfoCompat.Builder(context, id).setShortLabel(name).setLongLabel(name)
                .setIntent(launchIntent).setIcon(IconCompat.createFromIcon(context, icon)).build()

        if (shortcuts.any { it.id == id }) {
            return ShortcutManagerCompat.updateShortcuts(context, listOf(shortcut))
        }

        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)

    }

    /**
     * 创建动态快捷方式
     * @param context 上下文
     * @param name 快捷方式名称
     * @param id 快捷方式ID
     * @param icon 快捷方式图标
     * @param targetActivity 目标活动
     * @param scriptPath 脚本路径
     */
    @JvmStatic
    fun createDynamicShortcut(
        context: Context,
        name: CharSequence,
        id: String,
        icon: Icon,
        targetActivity: Class<*>,
        scriptPath: String?
    ): Boolean {
        val launchIntent = Intent(context, targetActivity).apply {
            action = Intent.ACTION_VIEW
            scriptPath?.let { putExtra(ScriptIntents.EXTRA_KEY_PATH, it) }
        }

        val shortcut =
            ShortcutInfoCompat.Builder(context, id).setShortLabel(name).setLongLabel(name)
                .setIntent(launchIntent).setIcon(IconCompat.createFromIcon(context, icon)).build()
        return try {
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        } catch (_: Exception) {
            false
        }
    }


    /**
     * 移除所有动态快捷方式
     */
    @JvmStatic
    fun removeAllShortcuts(context: Context) {
        val shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
        if (shortcuts.isNotEmpty()) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, shortcuts.map { it.id })
        }
    }
}