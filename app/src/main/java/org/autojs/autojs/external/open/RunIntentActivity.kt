package org.autojs.autojs.external.open

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stardust.autojs.script.StringScriptSource
import com.stardust.pio.PFiles
import org.autojs.autojs.R
import org.autojs.autojs.external.ScriptIntents
import org.autojs.autojs.model.script.Scripts
import timber.log.Timber
import java.io.FileNotFoundException

/**
 * Created by Stardust on 2017/2/22.
 */
class RunIntentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            handleIntent(intent)
        } catch (e: Exception) {
            Timber.e(e, "处理 Intent 时发生错误")
            Toast.makeText(this, R.string.edit_and_run_handle_intent_error, Toast.LENGTH_LONG)
                .show()
        }
        finish()
    }

    @Throws(FileNotFoundException::class)
    private fun handleIntent(intent: Intent) {
        val uri = intent.data
        if (uri != null && "content" == uri.scheme) {
            val stream = contentResolver.openInputStream(uri)
            Scripts.run(StringScriptSource(PFiles.read(stream)))
        } else {
            ScriptIntents.handleIntent(this, intent)
        }
    }
}
