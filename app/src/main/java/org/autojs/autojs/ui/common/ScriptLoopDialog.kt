package org.autojs.autojs.ui.common

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.app.DialogUtils
import com.stardust.app.GlobalAppContext
import org.autojs.autojs.R
import org.autojs.autojs.databinding.DialogScriptLoopBinding
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.model.script.Scripts

/**
 * Created by Stardust on 2017/7/8.
 */

class ScriptLoopDialog(context: Context, private val scriptFile: ScriptFile) {

    private val binding =
        DialogScriptLoopBinding.inflate(android.view.LayoutInflater.from(context), null, false)
    private val dialog = MaterialDialog.Builder(context)
        .title(R.string.text_run_repeatedly)
        .customView(binding.root, true)
        .positiveText(R.string.ok)
        .onPositive { _, _ -> startScriptRunningLoop() }
        .build()

    private fun startScriptRunningLoop() {
        try {
            val loopTimes = binding.loopTimes.text.toString().toInt()
            val loopInterval = binding.loopInterval.text.toString().toFloat()
            val loopDelay = binding.loopDelay.text.toString().toFloat()
            Scripts.runRepeatedly(
                scriptFile,
                loopTimes,
                (1000L * loopDelay).toLong(),
                (loopInterval * 1000L).toLong()
            )
        } catch (_: NumberFormatException) {
            GlobalAppContext.toast(R.string.text_number_format_error)
        }
    }

    fun windowType(windowType: Int): ScriptLoopDialog {
        val window = dialog.window
        if (window != null) {
            window.setType(windowType)
        }
        return this
    }

    fun show() {
        DialogUtils.showDialog(dialog)
    }

}
