package org.autojs.autojs.ui.edit

import android.content.Context
import android.view.LayoutInflater
import android.widget.SeekBar
import com.afollestad.materialdialogs.MaterialDialog
import org.autojs.autojs.R
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder
import org.autojs.autojs.databinding.DialogTextSizeSettingBinding

/**
 * Created by Stardust on 2018/2/24.
 */

class TextSizeSettingDialogBuilder(context: Context) : ThemeColorMaterialDialogBuilder(context), SeekBar.OnSeekBarChangeListener {

    interface PositiveCallback {
        fun onPositive(value: Int)
    }

    private companion object {
        const val MIN = 8
    }

    private val binding: DialogTextSizeSettingBinding = DialogTextSizeSettingBinding.inflate(LayoutInflater.from(context))
    private var textSize = 0
    private var materialDialog: MaterialDialog? = null

    init {
        customView(binding.root, false)
        title(R.string.text_text_size)
        positiveText(R.string.ok)
        negativeText(R.string.cancel)
        binding.seekbar.setOnSeekBarChangeListener(this)
    }

    private fun setTextSize(textSize: Int) {
        this.textSize = textSize
        val title = context.getString(R.string.text_size_current_value, textSize)
        if (materialDialog != null) {
            materialDialog?.setTitle(title)
        } else {
            title(title)
        }
        binding.previewText.textSize = textSize.toFloat()
    }

    fun initialValue(value: Int): TextSizeSettingDialogBuilder {
        binding.seekbar.progress = value - MIN
        return this
    }

    fun callback(callback: PositiveCallback): TextSizeSettingDialogBuilder {
        onPositive { _, _ -> callback.onPositive(textSize) }
        return this
    }

    override fun build(): MaterialDialog {
        val dialog = super.build()
        materialDialog = dialog
        return dialog
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        setTextSize(progress + MIN)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        // Do nothing
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        // Do nothing
    }
}
