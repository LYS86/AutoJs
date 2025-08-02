package org.autojs.autojs.tool

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import kotlin.math.max

object BitmapTool {

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val size = max(width, height)

        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val left = (size - width) / 2
        val top = (size - height) / 2

        drawable.setBounds(left, top, left + width, top + height)
        drawable.draw(canvas)

        return bitmap
    }

    @JvmStatic
    fun drawableToBitmapIfNeeded(drawable: Drawable): Bitmap {
        return if (drawable is BitmapDrawable) {
            drawable.bitmap ?: drawableToBitmap(drawable)
        } else {
            drawableToBitmap(drawable)
        }
    }
}
