package org.autojs.autojs.ui.doc

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.afollestad.materialdialogs.MaterialDialog
import org.autojs.autojs.databinding.FloatingManualDialogBinding

class ManualDialog(context: Context) {

    private val binding = FloatingManualDialogBinding.inflate(
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as android.view.LayoutInflater
    )
    private val dialog: Dialog = MaterialDialog.Builder(context)
        .customView(binding.root, false)
        .build()
        .apply {
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

    init {
        binding.close.setOnClickListener { dialog.dismiss() }
        binding.fullscreen.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(binding.root.context, DocumentationActivity::class.java)
                .putExtra(DocumentationActivity.EXTRA_URL, binding.ewebView.webView.url)
            binding.root.context.startActivity(intent)
        }
    }

    fun title(title: String): ManualDialog {
        binding.title.text = title
        return this
    }

    fun url(url: String): ManualDialog {
        binding.ewebView.webView.loadUrl(url)
        return this
    }

    fun pinToLeft(listener: android.view.View.OnClickListener): ManualDialog {
        binding.pinToLeft.setOnClickListener {
            dialog.dismiss()
            listener.onClick(it)
        }
        return this
    }

    fun show(): ManualDialog {
        dialog.show()
        return this
    }
}
