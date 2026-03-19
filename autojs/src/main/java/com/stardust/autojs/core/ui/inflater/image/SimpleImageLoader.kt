package com.stardust.autojs.core.ui.inflater.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable
import timber.log.Timber
import java.net.URL

class SimpleImageLoader : ImageLoader {

    override fun loadInto(view: ImageView, uri: Uri) {
        loadDrawable(view, uri) { view.setImageDrawable(it) }
    }

    override fun loadIntoBackground(view: View, uri: Uri) {
        loadDrawable(view, uri) { view.background = it }
    }

    override fun load(view: View, uri: Uri): Drawable {
        val url = URL(uri.toString())
        val bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        return bmp.toDrawable(view.resources)
    }

    override fun loadDrawable(view: View, uri: Uri, callback: (Drawable) -> Unit) {
        Thread {
            try {
                val drawable = load(view, uri)
                view.post { callback(drawable) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun loadBitmap(view: View, uri: Uri, callback: (Bitmap) -> Unit) {
        Thread {
            try {
                val url = URL(uri.toString())
                val bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                view.post { callback(bmp) }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }.start()
    }
}
