package org.autojs.autojs.external.open

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.stardust.autojs.script.StringScriptSource
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.model.script.Scripts
import org.autojs.autojs.R
import timber.log.Timber
import java.io.FileNotFoundException

class RunIntentActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            handleIntent(intent)
        } catch (e: Exception) {
            Timber.e(e)
            AutoJs.getInstance().globalConsole.error(e.toString())
            Toast.makeText(this, R.string.edit_and_run_handle_intent_error, Toast.LENGTH_LONG).show()
        }
        finish()
    }

    @Throws(FileNotFoundException::class)
    private fun handleIntent(intent: Intent) {
        val uri: Uri? = intent.data
        if (uri != null && "content" == uri.scheme) {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                Scripts.run(StringScriptSource(reader.readText()))
            }
        } else {
            ScriptIntents.handleIntent(this, intent)
        }
    }
}
