package org.autojs.autojs.external.shortcut

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.model.script.PathChecker
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.model.script.Scripts

class ShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(ScriptIntents.EXTRA_KEY_PATH)
        if (PathChecker(this).checkAndToastError(path)) {
            runScriptFile(path)
        }
        finish()
    }

    private fun runScriptFile(path: String?) {
        if (path == null) return
        try {
            Scripts.run(ScriptFile(path))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }
}