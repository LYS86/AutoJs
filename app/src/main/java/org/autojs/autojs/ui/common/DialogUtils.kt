package org.autojs.autojs.ui.common

import android.content.Context
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog

object DialogUtils {

    fun showBasic(
        context: Context,
        title: String? = null,
        content: String,
        positiveText: String = context.getString(android.R.string.ok),
        callback: (() -> Unit)? = null
    ): MaterialDialog {
        return MaterialDialog.Builder(context)
            .apply {
                if (title != null) title(title)
                content(content)
                positiveText(positiveText)
                callback?.let {
                    onPositive { _: MaterialDialog, _: DialogAction -> it() }
                }
            }
            .build()
            .also { it.show() }
    }

    fun showConfirm(
        context: Context,
        title: String? = null,
        content: String,
        positiveText: String = context.getString(android.R.string.ok),
        negativeText: String = context.getString(android.R.string.cancel),
        onPositive: (() -> Unit)? = null,
        onNegative: (() -> Unit)? = null
    ): MaterialDialog {
        return MaterialDialog.Builder(context)
            .apply {
                if (title != null) title(title)
                content(content)
                positiveText(positiveText)
                negativeText(negativeText)
                onPositive?.let {
                    onPositive { _: MaterialDialog, _: DialogAction -> it() }
                }
                onNegative?.let {
                    onNegative { _: MaterialDialog, _: DialogAction -> it() }
                }
            }
            .build()
            .also { it.show() }
    }

    fun custom(context: Context): MaterialDialog.Builder {
        return MaterialDialog.Builder(context)
    }
}