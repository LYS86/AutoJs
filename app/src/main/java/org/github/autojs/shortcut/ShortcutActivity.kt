package org.github.autojs.shortcut

import android.app.Activity
import android.os.Bundle
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.model.script.PathChecker
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.model.script.Scripts

class ShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(ScriptIntents.EXTRA_KEY_PATH)
        if (path != null && PathChecker(this).checkAndToastError(path)) {
            Scripts.run(ScriptFile(path))
        }
        finish()
    }
}
