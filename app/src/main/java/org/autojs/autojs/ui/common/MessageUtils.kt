package org.autojs.autojs.ui.common

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar

object MessageUtils {

    // 基础消息显示
    fun show(context: Context?, view: View?, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        when {
            view != null -> Snackbar.make(view, message, duration).show()
            context != null -> Toast.makeText(context, message, duration.toToastDuration()).show()
        }
    }

    // 带资源ID的版本
    fun show(context: Context?, view: View?, @StringRes resId: Int, duration: Int = Snackbar.LENGTH_SHORT) {
        when {
            view != null -> Snackbar.make(view, resId, duration).show()
            context != null -> Toast.makeText(context, resId, duration.toToastDuration()).show()
        }
    }

    // 带Action的版本
    fun showWithAction(
        context: Context?,
        view: View?,
        message: String,
        actionText: String,
        duration: Int = Snackbar.LENGTH_LONG,
        action: (View) -> Unit = {}
    ) {
        when {
            view != null -> {
                Snackbar.make(view, message, duration)
                    .setAction(actionText, action)
                    .show()
            }
            context != null -> {
                Toast.makeText(context, message, duration.toToastDuration()).show()
            }
        }
    }

    // 转换Snackbar时长到Toast时长
    private fun Int.toToastDuration(): Int {
        return when (this) {
            Snackbar.LENGTH_LONG -> Toast.LENGTH_LONG
            else -> Toast.LENGTH_SHORT
        }
    }
}