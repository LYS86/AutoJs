package org.github.autojs.shortcut

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import org.autojs.autojs.external.ScriptIntents

object ShortcutManager {

    fun createPinnedShortcut(
        context: Context, name: CharSequence, id: String, icon: IconCompat, scriptPath: String
    ): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false
        }
        @SuppressLint("WrongConstant")
        val shortcuts = ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)

        val launchIntent = Intent(context, ShortcutActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(ScriptIntents.EXTRA_KEY_PATH, scriptPath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val shortcut =
            ShortcutInfoCompat.Builder(context, id).setShortLabel(name).setLongLabel(name).setIntent(launchIntent)
                .setIcon(icon).build()

        if (shortcuts.any { it.id == id }) {
            return ShortcutManagerCompat.updateShortcuts(context, listOf(shortcut))
        }

        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    fun createDynamicShortcut(
        context: Context, name: CharSequence, id: String, icon: IconCompat, scriptPath: String
    ): Boolean {
        val launchIntent = Intent(context, ShortcutActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(ScriptIntents.EXTRA_KEY_PATH, scriptPath)
        }

        val shortcut =
            ShortcutInfoCompat.Builder(context, id).setShortLabel(name).setLongLabel(name).setIntent(launchIntent)
                .setIcon(icon).build()
        return ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    fun removeAllShortcuts(context: Context) {
        val shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
        if (shortcuts.isNotEmpty()) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, shortcuts.map { it.id })
        }
    }
}
