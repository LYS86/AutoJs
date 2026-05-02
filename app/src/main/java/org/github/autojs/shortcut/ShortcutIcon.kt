package org.github.autojs.shortcut

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val EXTRA_PACKAGE_NAME = "extra_package_name"

suspend fun getBitmapFromIntent(context: Context, data: Intent): Bitmap = withContext(Dispatchers.IO) {
    val packageName = data.getStringExtra(EXTRA_PACKAGE_NAME)
    when {
        packageName != null -> context.packageManager.getApplicationIcon(packageName).toBitmap()
        data.data != null -> {
            val uri = data.data!!
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri))
                ?: throw IllegalArgumentException("failed to decode bitmap from uri")
        }
        else -> throw IllegalArgumentException("invalid intent")
    }
}
