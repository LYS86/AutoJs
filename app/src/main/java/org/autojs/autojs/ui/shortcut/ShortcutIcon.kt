@file:JvmName("ShortcutIcon")

package org.autojs.autojs.ui.shortcut

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toBitmap
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.IOException

fun Context.decodeShortcutIcon(intent: Intent): Single<Bitmap> = Single.fromCallable {
    val packageName = intent.getStringExtra(ShortcutIconSelectActivity.EXTRA_PACKAGE_NAME)
    if (packageName != null) {
        val drawable = packageManager.getApplicationIcon(packageName)
        drawable.toBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
    } else {
        val uri = intent.data ?: throw IllegalArgumentException("invalid intent")
        contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
            ?: throw IOException("cannot decode image: $uri")
    }
}.subscribeOn(Schedulers.io())
