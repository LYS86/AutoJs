package org.autojs.autojs.ui.common

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar

object MessageUtils {

    /**
     * 显示一个消息
     * @param context 上下文，建议传入 **Activity** 以便 Snackbar 查找父视图
     * @param view 用于 Snackbar 的锚视图，如果为空则使用Toast
     * @param message 消息
     * @param duration 持续时间,可选值：[Snackbar.LENGTH_INDEFINITE],[Snackbar.LENGTH_SHORT],[Snackbar.LENGTH_LONG]
     * @param forceToast 是否强制使用Toast
     */
    fun show(
        context: Context,
        view: View? = null,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        forceToast: Boolean = false
    ) {
        if (message.isBlank()) return
        when {
            forceToast -> showToast(context, message, duration)
            view?.isAttachedToWindow == true -> Snackbar.make(context, view, message, duration)
                .show()

            else -> showToast(context, message, duration)
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
        if (message.isBlank()) return
        when {
            view?.isAttachedToWindow == true -> {
                Snackbar.make(context,view, message, duration).setAction(actionText, action).show()
            }

            else -> showToast(context, message, duration.toToastDuration())
        }
    }

    private fun showToast(context: Context, message: String, duration: Int) {
        Toast.makeText(context, message, duration.toToastDuration()).show()
    }

    private fun Int.toToastDuration(): Int {
        return when (this) {
            Snackbar.LENGTH_LONG -> Toast.LENGTH_LONG
            Snackbar.LENGTH_INDEFINITE -> Toast.LENGTH_LONG

            else -> Toast.LENGTH_SHORT
        }
    }
}