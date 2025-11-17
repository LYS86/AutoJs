package org.autojs.autojs.ui.doc

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import androidx.core.graphics.drawable.toDrawable
import com.afollestad.materialdialogs.MaterialDialog
import org.autojs.autojs.databinding.FloatingManualDialogBinding


class ManualDialog {

    private val binding: FloatingManualDialogBinding
    private val dialog: MaterialDialog
    private val context: Context

    constructor(context: Context) {
        this.context = context
        binding = FloatingManualDialogBinding.inflate(LayoutInflater.from(context))

        dialog = MaterialDialog.Builder(context)
            .customView(binding.root, false)
            .build()
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        setupClickListeners()
    }


    private fun setupClickListeners() {
        binding.close.setOnClickListener { close() }
        binding.fullscreen.setOnClickListener { viewInNewActivity() }
    }

    fun title(title: String): ManualDialog {
        binding.title.text = title
        return this
    }

    fun url(url: String): ManualDialog {
        binding.ewebView.webView.loadUrl(url)
        return this
    }


    fun pinToLeft(listener: View.OnClickListener): ManualDialog {
        binding.pinToLeft.setOnClickListener { v ->
            dialog.dismiss()
            listener.onClick(v)
        }
        return this
    }


    fun show(): ManualDialog {
        dialog.show()
        return this
    }


    private fun close() {
        dialog.dismiss()
    }


    private fun viewInNewActivity() {
        dialog.dismiss()
        val intent = Intent(context, DocumentationActivity::class.java)
        intent.putExtra(DocumentationActivity.EXTRA_URL, binding.ewebView.webView.url)
        context.startActivity(intent)
    }
}