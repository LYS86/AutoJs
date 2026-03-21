package org.autojs.autojs.theme.dialog

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialAlertDialog(context: Context) : MaterialAlertDialogBuilder(
    ContextThemeWrapper( context, R.style.Theme_Material3_DayNight_Dialog)
)