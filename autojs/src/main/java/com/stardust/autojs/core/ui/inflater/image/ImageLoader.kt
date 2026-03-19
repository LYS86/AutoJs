package com.stardust.autojs.core.ui.inflater.image

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView

interface ImageLoader {

    fun loadInto(view: ImageView, uri: Uri)

    fun loadIntoBackground(view: View, uri: Uri)

    fun load(view: View, uri: Uri): Drawable

    fun loadDrawable(view: View, uri: Uri, callback: (Drawable) -> Unit)

    fun loadBitmap(view: View, uri: Uri, callback: (Bitmap) -> Unit)

}
