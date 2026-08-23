package org.autojs.autojs.external.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.autojs.autojs.external.ScriptIntents

object ShortcutManager {

    @JvmStatic
    fun createPinnedShortcut(
        context: Context, name: CharSequence, id: String, icon: IconCompat, scriptPath: String
    ): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false
        }
        val shortcut = buildShortcut(context, name, id, icon, scriptPath)
        val pinned = ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
        if (pinned.any { it.id == id }) {
            return ShortcutManagerCompat.updateShortcuts(context, listOf(shortcut))
        }
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    @JvmStatic
    fun createDynamicShortcut(
        context: Context, name: CharSequence, id: String, icon: IconCompat, scriptPath: String
    ): Boolean {
        val shortcut = buildShortcut(context, name, id, icon, scriptPath)
        return ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    private fun buildShortcut(
        context: Context, name: CharSequence, id: String, icon: IconCompat, scriptPath: String
    ): ShortcutInfoCompat {
        val launchIntent = Intent(context, ShortcutActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(ScriptIntents.EXTRA_KEY_PATH, scriptPath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(name)
            .setLongLabel(name)
            .setIntent(launchIntent)
            .setIcon(icon)
            .build()
    }
}
