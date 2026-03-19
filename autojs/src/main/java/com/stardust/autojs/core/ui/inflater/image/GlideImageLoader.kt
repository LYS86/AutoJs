package com.stardust.autojs.core.ui.inflater.image

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class GlideImageLoader : ImageLoader {

    override fun loadInto(view: ImageView, uri: Uri) {
        Glide.with(view)
            .load(uri)
            .into(view)
    }

    override fun loadIntoBackground(view: View, uri: Uri) {
        Glide.with(view)
            .load(uri)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    view.background = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    override fun load(view: View, uri: Uri): Drawable {
        throw UnsupportedOperationException()
    }

    override fun loadDrawable(view: View, uri: Uri, callback: (Drawable) -> Unit) {
        Glide.with(view)
            .load(uri)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    callback(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    override fun loadBitmap(view: View, uri: Uri, callback: (Bitmap) -> Unit) {
        Glide.with(view)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    callback(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }
}
