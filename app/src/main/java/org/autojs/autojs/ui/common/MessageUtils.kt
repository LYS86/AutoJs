package org.autojs.autojs.ui.common

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar

object MessageUtils {

    fun show(
        context: Context,
        view: View? = null,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        forceToast: Boolean = false
    ) {
        when {
            forceToast -> Toast.makeText(context, message, duration.toToastDuration()).show()
            view != null -> Snackbar.make(context,view, message, duration).show()
            else -> Toast.makeText(context, message, duration.toToastDuration()).show()
        }
    }



    fun show(
        context: Context,
        view: View?,
        @StringRes resId: Int,
        duration: Int = Snackbar.LENGTH_SHORT,
        forceToast: Boolean = false
    ) {
        show(context, view, context.getString(resId), duration, forceToast)
    }

    fun showWithAction(
        context: Context,
        view: View?,
        message: String,
        actionText: String,
        duration: Int = Snackbar.LENGTH_LONG,
        action: (View) -> Unit = {}
    ) {
        when {
            view != null -> {
                Snackbar.make(context,view, message, duration).setAction(actionText, action).show()
            }
            else -> Toast.makeText(context, message, duration.toToastDuration()).show()

        }
    }

    private fun Int.toToastDuration(): Int {
        return when (this) {
            Snackbar.LENGTH_LONG -> Toast.LENGTH_LONG
            else -> Toast.LENGTH_SHORT
        }
    }
}